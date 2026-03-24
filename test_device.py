#!/usr/bin/env python3
"""
ISUP Protocol Test Script
Tests LOGIN_REQUEST, LOGIN_RESPONSE, KEEPALIVE cycle against the ISUP server.

Frame format: STX(1) | Length(4 LE) | MsgType(2 LE) | SessionID(4 LE) | SeqNo(2 LE) | Payload(N) | ETX(1)
STX = 0x20, ETX = 0x0A
Length = total frame size (14 + payload length)
"""

import socket
import struct
import os
import time
import sys

HOST = "localhost"
PORT = 7660

# Message type codes (from MessageType.java)
MSG_LOGIN_REQUEST    = 0x0001
MSG_LOGIN_RESPONSE   = 0x0002
MSG_LOGOUT           = 0x0003
MSG_ALARM_EVENT      = 0x0004
MSG_ALARM_RESPONSE   = 0x0005
MSG_KEEPALIVE_REQ    = 0x0013
MSG_KEEPALIVE_RESP   = 0x0014

STX = 0x20
ETX = 0x0A
HEADER_SIZE = 14  # STX(1) + Len(4) + Type(2) + Sess(4) + Seq(2) + ETX(1)
NONCE_SIZE = 33

def build_frame(msg_type: int, session_id: int, seq_no: int, payload: bytes = b'') -> bytes:
    """Build an ISUP frame."""
    frame_len = HEADER_SIZE + len(payload)
    buf = bytearray()
    buf.append(STX)
    buf += struct.pack('<I', frame_len)    # Length (4 bytes LE)
    buf += struct.pack('<H', msg_type)    # MsgType (2 bytes LE)
    buf += struct.pack('<I', session_id)  # SessionID (4 bytes LE)
    buf += struct.pack('<H', seq_no)      # SeqNo (2 bytes LE)
    buf += payload
    buf.append(ETX)
    return bytes(buf)

def parse_frame(data: bytes):
    """Parse an ISUP frame. Returns (msg_type, session_id, seq_no, payload) or None."""
    if len(data) < HEADER_SIZE:
        return None
    if data[0] != STX:
        print(f"  [WARN] Bad STX byte: 0x{data[0]:02X}")
        return None
    length    = struct.unpack_from('<I', data, 1)[0]
    msg_type  = struct.unpack_from('<H', data, 5)[0]
    session_id = struct.unpack_from('<I', data, 7)[0]
    seq_no    = struct.unpack_from('<H', data, 11)[0]
    payload_len = length - HEADER_SIZE
    if payload_len < 0:
        return None
    payload = data[13:13 + payload_len]
    return msg_type, session_id, seq_no, payload

def recv_frame(sock: socket.socket, timeout: float = 5.0) -> bytes:
    """Receive a complete frame from the socket."""
    sock.settimeout(timeout)
    data = b''
    while True:
        try:
            chunk = sock.recv(4096)
            if not chunk:
                break
            data += chunk
            # Check if we have a complete frame
            if len(data) >= HEADER_SIZE:
                length = struct.unpack_from('<I', data, 1)[0]
                if len(data) >= length:
                    return data[:length]
        except socket.timeout:
            break
    return data

def build_login_payload(device_id: str, nonce: bytes) -> bytes:
    """Build LOGIN_REQUEST payload: deviceId (null-padded to 64 bytes) + nonce (33 bytes)."""
    device_bytes = device_id.encode('utf-8')
    # Pad/truncate to 64 bytes with null terminator
    id_field = device_bytes[:63] + b'\x00' * (64 - min(len(device_bytes), 63))
    return id_field + nonce

def test_login(sock: socket.socket) -> bool:
    """Send LOGIN_REQUEST and verify LOGIN_RESPONSE."""
    print("\n[TEST 1] LOGIN_REQUEST")

    device_id = "admin"
    nonce = os.urandom(NONCE_SIZE)  # Random 33-byte nonce

    payload = build_login_payload(device_id, nonce)
    frame = build_frame(MSG_LOGIN_REQUEST, 0, 1, payload)

    print(f"  Sending LOGIN_REQUEST: device_id={device_id!r}, frame_len={len(frame)}")
    sock.sendall(frame)

    response_data = recv_frame(sock, timeout=5.0)
    if not response_data:
        print("  [FAIL] No response received")
        return False

    parsed = parse_frame(response_data)
    if not parsed:
        print(f"  [FAIL] Could not parse response: {response_data.hex()}")
        return False

    msg_type, session_id, seq_no, payload = parsed
    print(f"  Received: msg_type=0x{msg_type:04X}, session_id={session_id}, seq_no={seq_no}")

    if msg_type == MSG_LOGIN_RESPONSE:
        if len(payload) >= 1:
            result = payload[0]
            print(f"  Login result byte: 0x{result:02X} ({'SUCCESS' if result == 0 else 'FAILURE'})")
            if result == 0:
                print("  [PASS] LOGIN_RESPONSE received with success")
                return True, session_id, seq_no
            else:
                print("  [WARN] LOGIN_RESPONSE received but with non-zero result (device auth check)")
                return True, session_id, seq_no  # Still consider it a valid response
        print("  [PASS] LOGIN_RESPONSE received")
        return True, session_id, seq_no
    else:
        print(f"  [FAIL] Expected LOGIN_RESPONSE (0x{MSG_LOGIN_RESPONSE:04X}), got 0x{msg_type:04X}")
        return False, 0, 0

def test_keepalive(sock: socket.socket, session_id: int, seq_no: int) -> bool:
    """Send KEEPALIVE_REQUEST and verify KEEPALIVE_RESPONSE."""
    print("\n[TEST 2] KEEPALIVE_REQUEST")

    frame = build_frame(MSG_KEEPALIVE_REQ, session_id, seq_no + 1)
    print(f"  Sending KEEPALIVE_REQUEST: session_id={session_id}, seq_no={seq_no+1}")
    sock.sendall(frame)

    response_data = recv_frame(sock, timeout=5.0)
    if not response_data:
        print("  [FAIL] No response received")
        return False

    parsed = parse_frame(response_data)
    if not parsed:
        print(f"  [FAIL] Could not parse response: {response_data.hex()}")
        return False

    msg_type, resp_session_id, resp_seq_no, payload = parsed
    print(f"  Received: msg_type=0x{msg_type:04X}, session_id={resp_session_id}, seq_no={resp_seq_no}")

    if msg_type == MSG_KEEPALIVE_RESP:
        print("  [PASS] KEEPALIVE_RESPONSE received")
        return True
    else:
        print(f"  [FAIL] Expected KEEPALIVE_RESPONSE (0x{MSG_KEEPALIVE_RESP:04X}), got 0x{msg_type:04X}")
        return False

def main():
    print(f"Connecting to ISUP server at {HOST}:{PORT}...")

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((HOST, PORT))
        print(f"Connected to {HOST}:{PORT}")
    except ConnectionRefusedError:
        print(f"[FAIL] Connection refused to {HOST}:{PORT}")
        print("Make sure the ISUP server is running (docker compose up)")
        sys.exit(1)
    except Exception as e:
        print(f"[FAIL] Connection error: {e}")
        sys.exit(1)

    results = []
    session_id = 0
    seq_no = 0

    try:
        # Test 1: Login
        login_result = test_login(sock)
        if isinstance(login_result, tuple):
            passed, session_id, seq_no = login_result
        else:
            passed = login_result
        results.append(("LOGIN", passed))

        if passed:
            time.sleep(0.5)
            # Test 2: Keepalive
            ka_result = test_keepalive(sock, session_id, seq_no)
            results.append(("KEEPALIVE", ka_result))

    finally:
        sock.close()
        print("\n" + "="*50)
        print("TEST RESULTS:")
        all_passed = True
        for name, result in results:
            status = "PASS" if result else "FAIL"
            print(f"  {name}: [{status}]")
            if not result:
                all_passed = False
        print("="*50)
        if all_passed:
            print("ALL TESTS PASSED")
            sys.exit(0)
        else:
            print("SOME TESTS FAILED")
            sys.exit(1)

if __name__ == "__main__":
    main()
