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

high_scores = [340, 820, 150, 999, 430]

print(selection_sort(high_scores))
print(high_scores)
