import hashlib
import hmac

def hmac_sha256(key, data):
    return hmac.new(key, data, hashlib.sha256).hexdigest().upper()

# from the log
target = "1B969047051DE8932F307905D39339471C7FE5D1C5D83495A2145105F066220D"
# full 33 bytes
nonce_hex = "301B969047051DE8932F307905D39339471C7FE5D1C5D83495A2145105F066220D"
nonce_bytes = bytes.fromhex(nonce_hex)

# Skip the first byte
challenge_32 = nonce_bytes[1:33]

passwords = ["isup1234", "12345", "123456", "admin123"]

for pwd in passwords:
    pwd_bytes = pwd.encode()
    pwd_md5 = hashlib.md5(pwd_bytes).digest()
    
    variants = [
        ("V2_offset1", hmac_sha256(pwd_bytes, challenge_32)),
        ("V1_offset1", hmac_sha256(pwd_md5, challenge_32))
    ]
    
    for name, result in variants:
        if result == target:
            print(f"MATCH FOUND! {name} with password '{pwd}' -> {result}")
        else:
            print(f"NO MATCH: {name} / {pwd} -> {result}")
