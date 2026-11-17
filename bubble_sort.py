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


high_scores = [340, 820, 150, 999, 430]
print(bubble_sort(high_scores))
print(high_scores)