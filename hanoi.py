def hanoi(n , source, target, aux):
    if n == 0:
        return
    hanoi(n -1, source, aux, target)
    print(f"Move Disk {n} from {source} to {target}")
    hanoi(n - 1, aux, target, source)


hanoi(3, "A", "C", "B")
        
