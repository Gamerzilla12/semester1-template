import time

def linear_search_full_scan(items, target):
    """Deliberately naive: keeps scanning even after finding the target."""
    found_index = -1
    for i, item in enumerate(items):
        if item == target:
            found_index = i
    return found_index


def linear_search_early_exit(items, target):
    for i, item in enumerate(items):
        if item == target:
            return i
    return -1


def binary_search(items, target):
    loop = 0
    start = 0
    end = len(items) -1
    while start <= end:
        loop +=1
        mid = start + (end - start) // 2
        #print(mid)
        if items[mid] == target:
            print(f"times through {loop}")
            return mid
        elif mid < target:
            start = mid + 1
        else:
            end = mid - 1
    return -1

items = list(range(10000000))

tick=time.perf_counter()
print(linear_search_early_exit(items, 123400))
print(f'time {(time.perf_counter() - tick) * 1000}')


tick=time.perf_counter()
print(binary_search(items, 123400))
print(f'time {(time.perf_counter() - tick) * 1000}')


