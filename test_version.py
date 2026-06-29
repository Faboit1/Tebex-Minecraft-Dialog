import re
def is_1_21_6_or_above(version):
    match = re.search(r'1\.(\d+)\.(\d+)', version)
    if match:
        minor = int(match.group(1))
        patch = int(match.group(2))
        if minor > 21:
            return True
        if minor == 21 and patch >= 6:
            return True
    return False

print(is_1_21_6_or_above("1.21.6-R0.1-SNAPSHOT"))
print(is_1_21_6_or_above("1.21.4-R0.1-SNAPSHOT"))
print(is_1_21_6_or_above("1.22.0-R0.1-SNAPSHOT"))
