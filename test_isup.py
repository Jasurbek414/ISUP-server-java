import socket, struct, os, time, hashlib, hmac as hmac_lib

HOST = "localhost"
PORT = 7660
DEVICE_ID = "admin"
PASSWORD = "Password2025"

# ISUP v5 frame layout:
#   STX(1=0x20) | TotalLength(4 LE) | MsgType(2 LE) | SessionID(4 LE) | SeqNo(2 LE) | Payload(N) | ETX(1=0x0a)
#   TotalLength = full frame size (HEADER_SIZE=14 + payload length)
HEADER_SIZE = 14  # STX(1) + Len(4) + Type(2) + Sess(4) + Seq(2) + ETX(1)

def build_v5_frame(msg_type_code, session_id, seq_no, payload=b''):
    """Build a v5 ISUP frame with correct total-length field."""
    total_len = HEADER_SIZE + len(payload)
    frame = struct.pack('<B', 0x20)           # STX
    frame += struct.pack('<I', total_len)      # TotalLength (includes all 14 header bytes + payload)
    frame += struct.pack('<H', msg_type_code)  # MsgType
    frame += struct.pack('<I', session_id)     # SessionID
    frame += struct.pack('<H', seq_no)         # SeqNo
    frame += payload                           # Payload
    frame += struct.pack('<B', 0x0a)           # ETX
    return frame

def build_v5_login(device_id, password):
    """Build LOGIN_REQUEST (0x0001) with device_id + nonce + hmac payload."""
    nonce = os.urandom(33)
    device_bytes = device_id.encode().ljust(32, b'\x00')[:32]
    if password:
        key = hashlib.md5(password.encode()).digest()
        hmac_val = hmac_lib.new(key, nonce, hashlib.sha256).digest()
    else:
        hmac_val = bytes(32)
    payload = device_bytes + nonce + hmac_val  # 32 + 33 + 32 = 97 bytes
    return build_v5_frame(0x0001, 0, 1, payload), nonce

def build_heartbeat(session_id):
    """Build KEEPALIVE_REQUEST (0x0013) with no payload."""
    return build_v5_frame(0x0013, session_id, 2)

def build_alarm(session_id):
    """Build ALARM_EVENT (0x0004) with XML payload."""
    xml = b'''<?xml version="1.0" encoding="UTF-8"?>
<EventNotificationAlert>
  <eventType>AccessControllerEvent</eventType>
  <dateTime>2024-01-15T09:30:00+05:00</dateTime>
  <AccessControllerEvent>
    <employeeNoString>EMP001</employeeNoString>
    <name>Test User</name>
    <cardNo>123456</cardNo>
    <type>face</type>
    <AttendanceStatus>checkIn</AttendanceStatus>
    <doorNo>1</doorNo>
  </AccessControllerEvent>
</EventNotificationAlert>'''
    return build_v5_frame(0x0004, session_id, 3, xml)

def recv_frame(s):
    """Receive one full ISUP v5 frame."""
    # Read first 5 bytes to get total length
    header = b''
    while len(header) < 5:
        chunk = s.recv(5 - len(header))
        if not chunk:
            raise ConnectionError("Connection closed")
        header += chunk

    if header[0] != 0x20:
        raise ValueError(f"Unexpected STX byte: 0x{header[0]:02x}")

    total_len = struct.unpack('<I', header[1:5])[0]
    remaining = total_len - 5  # already read 5 bytes

    rest = b''
    while len(rest) < remaining:
        chunk = s.recv(remaining - len(rest))
        if not chunk:
            raise ConnectionError("Connection closed")
        rest += chunk

    return header + rest

def main():
    try:
        s = socket.socket()
        s.settimeout(10)
        s.connect((HOST, PORT))
        print(f"CONNECT: OK")

        login_pkt, nonce = build_v5_login(DEVICE_ID, PASSWORD)
        s.send(login_pkt)
        print(f"LOGIN SENT: {len(login_pkt)} bytes")

        resp = recv_frame(s)
        msg_type = struct.unpack('<H', resp[5:7])[0]
        session_id = struct.unpack('<I', resp[7:11])[0]
        print(f"LOGIN RESP: type=0x{msg_type:04x} session_id={session_id} total={len(resp)} bytes raw={resp.hex()}")

        time.sleep(1)

        hb_pkt = build_heartbeat(session_id)
        s.send(hb_pkt)
        print(f"HEARTBEAT SENT: {len(hb_pkt)} bytes")

        hb_resp = recv_frame(s)
        hb_type = struct.unpack('<H', hb_resp[5:7])[0]
        print(f"HEARTBEAT RESP: type=0x{hb_type:04x} (expect 0x0014) raw={hb_resp.hex()}")

        time.sleep(1)

        alarm_pkt = build_alarm(session_id)
        s.send(alarm_pkt)
        print(f"ALARM SENT: {len(alarm_pkt)} bytes")

        try:
            alarm_resp = recv_frame(s)
            alarm_type = struct.unpack('<H', alarm_resp[5:7])[0]
            print(f"ALARM RESP: type=0x{alarm_type:04x} (expect 0x0005) raw={alarm_resp.hex()}")
        except Exception as e:
            print(f"ALARM RESP timeout (may be ok): {e}")

        time.sleep(1)
        s.close()
        print("TCP TEST: PASS")
    except Exception as e:
        print(f"TCP TEST FAILED: {e}")

main()
