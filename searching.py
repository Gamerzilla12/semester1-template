import random
import time

def linear_search(items, target):
    for i, item in enumerate(items):
        if item == target:
            return i
    return -1

def binary_search(sorted_items, target):
    low, high = 0, len(sorted_items) - 1
    while low <= high:
        mid = (low + high) // 2
        if sorted_items[mid] == target:
            return mid
        elif sorted_items[mid] < target:
            low = mid + 1
        else:
            high = mid - 1
    return -1


lst = list(range(10_000_000))
random.shuffle(lst)

print("beginning run")
begin = time.perf_counter()
print(linear_search(lst, 8_800_000))
print(f"Took {time.perf_counter() - begin:.3f}s")

begin = time.perf_counter()
lst.sort()
print(binary_search(lst, 8_800_000))
print(f"Took {time.perf_counter() - begin:.3f}s")