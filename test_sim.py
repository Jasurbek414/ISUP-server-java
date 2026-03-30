import socket
import time
import struct

def test_login():
    # Simulation: STX=0x10, Version=1.0, Cmd=LOGIN_REQUEST (0x01)
    # Body: Serial + Model
    serial = b"TESTSERIAL123"
    model = b"DS-K1T343"
    body = b"\x01\x01\x00" + bytes([len(serial)]) + serial + bytes([len(model)]) + model
    payload = b"\x10" + bytes([len(body)+1]) + b"\x01" + body

    print(f"Sending dummy login: {payload.hex()}")
    
    with socket.create_connection(("127.0.0.1", 7660), timeout=5) as s:
        s.sendall(payload)
        resp = s.recv(1024)
        print(f"Server response: {resp.hex()}")
        
        # Wait for XML burst
        time.sleep(0.5)
        burst1 = s.recv(1024)
        print(f"Burst 1 (XML/Binary): {burst1.hex()}")

if __name__ == "__main__":
    test_login()
