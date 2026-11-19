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

high_scores = [340, 820, 150, 999, 430]
insert_sort(high_scores)

import time

def time_sort(sort_fn, data):
    start = time.perf_counter()
    sort_fn(data)
    return time.perf_counter() - start

sorted_list = list(range(2000, 0, -1))
reverse_list = list(range(2000))

print("Already Sorted:", time_sort(insert_sort, sorted_list))
print("Reverse Order: ", time_sort(insert_sort, reverse_list))
