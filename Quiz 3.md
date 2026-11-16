1. Identify the base case and the recursive case:
```python
def mystery(n):
    if n == 0:
        return 1
    return n * mystery(n - 1)
```

2. Why does naive recursive Fibonacci become so slow for larger n?


3. What does memoization do, and what's the tradeoff?


4. What makes a recursive call "tail position," and why does that matter in Clojure?

5. Short answer: explain "recursive backtracking" in your own words, using the maze generator as your example.
