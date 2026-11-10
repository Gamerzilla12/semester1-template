
## Day 41 — Lab: Recursion Depth Limits (Python vs. Clojure)

**Objectives**
- Empirically find where each language's plain recursion breaks
- Understand the real difference `recur` makes for stack safety

**Python — finding the crash point**
```python
import sys

def recurse(n):
    if n <= 0:
        return 0
    return 1 + recurse(n - 1)

print("Python recursion limit:", sys.getrecursionlimit())

try:
    print(recurse(2000))
except RecursionError as e:
    print("Hit RecursionError:", e)

# You can raise the limit, but it's a band-aid, not a fix --
# it just moves the crash point further out, and risks crashing
# the whole process instead of raising a catchable error.
sys.setrecursionlimit(5000)
print(recurse(3000))
```

**Clojure — plain recursion vs. `recur`**
```clojure
;; Plain (non-tail) recursion also has a real limit -- the JVM's call stack
(defn recurse [n]
  (if (<= n 0)
    0
    (inc (recurse (dec n)))))

(recurse 2000)  ; likely fine
;; (recurse 100000)  ; would likely throw a StackOverflowError -- risky to run live!

;; recur avoids the problem entirely -- constant stack space, no growth at all
(defn recurse-tail [n]
  (loop [i n acc 0]
    (if (<= i 0)
      acc
      (recur (dec i) (inc acc)))))

(recurse-tail 1000000)  ;=> 1000000, no problem whatsoever
```