import socket
import xml.etree.ElementTree as ET

# Target Device info from SADP discovery
IP = "192.168.1.19"
MAC = "88-de-39-3c-83-1a"
PASS = "Password2026"

def modify_isup_sadp():
    # SADP Multicast Address
    MCAST_GRP = '239.255.255.250'
    MCAST_PORT = 37020
    
    # SADP XML Body for Modification (V5.0 compliant)
    # We set ehome_addressing_type to hostname, ehome_host_name to fake-faceid.uzinc.uz, 
    # and ehome_port to 17660
    
    msg = f"""<?xml version="1.0" encoding="utf-8"?>
    <NetConfig>
        <Uuid>D76B13F1-6385-48CE-BA41-1194E3870624</Uuid>
        <Types>modify</Types>
        <MAC>{MAC}</MAC>
        <Password>{PASS}</Password>
        <EHomeVer>5.0</EHomeVer>
        <EHomeAddressType>hostname</EHomeAddressType>
        <EHomeAddress>fake-faceid.uzinc.uz</EHomeAddress>
        <EHomePort>17660</EHomePort>
    </NetConfig>"""

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    sock.settimeout(5)
    sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)
    
    try:
        sock.sendto(msg.encode(), (MCAST_GRP, MCAST_PORT))
        print(f"SADP Modify sent to {IP} (MAC: {MAC}). Waiting for ACK...")
        
        # Wait for Response
        data, addr = sock.recvfrom(2048)
        print(f"Response from {addr}: {data.decode()}")
    except socket.timeout:
        print("SADP Modify Done (Timeout - No Response, but may have applied).")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    modify_isup_sadp()
