import time
import random

def time_sort(sort_fn, data):
    start = time.perf_counter()
    sort_fn(data)
    return time.perf_counter() - start

def selection_sort(scores):
    scores = scores.copy()
    n = len(scores)
    for i in range(n):
        best_idx = i
        for j in range(i + 1, n):
            if scores[j] > scores[best_idx]: #sorting in descending order.
                best_idx = j
        scores[i], scores[best_idx] = scores[best_idx], scores[i]
    return scores

def insert_sort(scores):
    scores = scores.copy()
    for i in range(1, len(scores)):
        key = scores[i]
        j = i - 1
        while j >= 0 and scores[j] < key: #shift smaller scores to the right
            scores[j + 1] = scores[j]
            j = j - 1
        scores[j + 1] = key
        #print(scores)
    return scores

def bubble_sort(scores): # time complexity O(n^2)
    scores = scores.copy() # don't mutate the orig
    n = len(scores)
    for i in range(n): # n
        swapped = False
        for j in range (n - 1 - i): # n * n
            if scores[j] < scores[j + 1]: # descending: highest first
                scores[j], scores[j + 1] = scores[j + 1], scores[j]
                swapped = True
        if not swapped:
            break # the list is sorted
    return scores


for n in [200, 400, 800, 1600]:
    data = [random.randint(0, 10000) for _ in range(n)]
    bubble_time = time_sort(bubble_sort, data)
    selection_time = time_sort(selection_sort, data)
    insert_time = time_sort(insert_sort, data)

    print(f'n={n} bubble={bubble_time:.5f}s selection={selection_time:.5f}s insert={insert_time:.5f}s')


already_sorted = sorted(range(1000), reverse=True)   # descending: our sort order
random_data = [random.randint(0, 10000) for _ in range(1000)]

print(f"Insertion sort, already-sorted input: {time_sort(insert_sort, already_sorted):.5f}s")
print(f"Insertion sort, random input: {time_sort(insert_sort, random_data):.5f}s")
print(f"Bubble sort, already-sorted input: {time_sort(bubble_sort, already_sorted):.5f}s")
print(f"Bubble sort, random input: {time_sort(bubble_sort, random_data):.5f}s")
print(f"Selection sort, already-sorted input: {time_sort(selection_sort, already_sorted):.5f}s")
print(f"Selection sort, random input: {time_sort(selection_sort, random_data):.5f}s")