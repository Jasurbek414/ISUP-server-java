#!/usr/bin/env python3
"""
Raw TCP listener to capture exactly what Hikvision device sends on port 7661.
We use a different port to not conflict with the running server.
"""
import socket
import time

HOST = '0.0.0.0'
PORT = 7661  # temporary capture port

srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
srv.bind((HOST, PORT))
srv.listen(1)
srv.settimeout(60)

print(f"[*] Listening on {HOST}:{PORT} for 60 seconds...")
print("[*] Configure device to send to port 7661 temporarily")
print()

try:
    conn, addr = srv.accept()
    print(f"[+] Connection from {addr}")
    conn.settimeout(15)
    
    all_data = b''
    while True:
        try:
            chunk = conn.recv(4096)
            if not chunk:
                break
            all_data += chunk
            print(f"[+] Received {len(chunk)} bytes:")
            print(f"    HEX: {chunk.hex()}")
            print(f"    RAW: {chunk[:200]}")
            
            # Parse ISUP header
            if len(all_data) >= 14:
                stx = all_data[0]
                length = int.from_bytes(all_data[1:5], 'little')
                msg_type = int.from_bytes(all_data[5:7], 'little')
                session_id = int.from_bytes(all_data[7:11], 'little')
                seq_no = int.from_bytes(all_data[11:13], 'little')
                print(f"\n[PARSED ISUP HEADER]")
                print(f"  STX:        0x{stx:02X} (expected 0x20)")
                print(f"  Length:     {length} (frame total)")
                print(f"  MsgType:    0x{msg_type:04X}")
                print(f"  SessionID:  {session_id}")
                print(f"  SeqNo:      {seq_no}")
                if len(all_data) > 13:
                    payload = all_data[13:]
                    print(f"  Payload({len(payload)} bytes): {payload[:50].hex()}")
                    # Try to decode payload as string
                    try:
                        text = payload.decode('utf-8', errors='replace')
                        print(f"  PayloadStr: {text[:100]}")
                    except:
                        pass
        except socket.timeout:
            break
    
    print(f"\n[=] Total received: {len(all_data)} bytes")
    print(f"[=] Full hex dump:")
    for i in range(0, min(len(all_data), 256), 16):
        line = all_data[i:i+16]
        hex_part = ' '.join(f'{b:02X}' for b in line)
        asc_part = ''.join(chr(b) if 32 <= b < 127 else '.' for b in line)
        print(f"  {i:04X}: {hex_part:<48}  {asc_part}")
    
    conn.close()
except socket.timeout:
    print("[-] No connection received in 60 seconds")
finally:
    srv.close()
