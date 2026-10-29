
def countdown(n, depth=0):
    indent = "  " * depth
    print(f"{indent}-> countdown({n}) called")
    if n <= 0:
        print(f"{indent}<- Liftoff!")
        return
    print(f"{indent}  printing {n}")
    countdown(n - 1, depth + 1)
    print(f"{indent}<- countdown({n}) returning")


countdown(30)