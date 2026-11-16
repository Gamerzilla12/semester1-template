import random

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
        elif sorted_items[mid] > target:
            low = mid + 1
        else:
            high = mid - 1
    return -1

leaderboard = [500, 480, 450, 420, 390, 350, 300, 275, 200, 150]  # sorted descending
print(leaderboard)
print(binary_search(leaderboard, 420))
random.shuffle(leaderboard)
print(leaderboard)
print(linear_search(leaderboard, 420))