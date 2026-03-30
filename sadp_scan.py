import socket
import xml.etree.ElementTree as ET

def sadp_scan():
    # SADP Multicast Address
    MCAST_GRP = '239.255.255.250'
    MCAST_PORT = 37020
    
    # Discovery Message
    msg = """<?xml version="1.0" encoding="utf-8"?><Probe><Uuid>D76B13F1-6385-48CE-BA41-1194E3870624</Uuid><Types>inquiry</Types></Probe>"""
    
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    sock.settimeout(5)
    sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)
    
    try:
        sock.sendto(msg.encode(), (MCAST_GRP, MCAST_PORT))
        print("SADP Probe sent. Waiting for responses...")
        
        while True:
            data, addr = sock.recvfrom(2048)
            print(f"Response from {addr}: {data.decode()}")
            
    except socket.timeout:
        print("SADP Scan Done.")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    sadp_scan()
