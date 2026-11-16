## Day 32 — Quiz: Linear Structures

**Sample Quiz (with answer key for the teacher)**

1. What data structure would you use to implement "undo," and why?



2. What's the time complexity of Python's `list.insert(0, x)`? Why?


3. In Clojure, does `(conj my-vector :x)` change `my-vector`? What does it return instead?


4. Trace through this code and give the final printed output:
```python
class Node:
    def __init__(self, value, next_node=None):
        self.value = value
        self.next = next_node

n3 = Node("c")
n2 = Node("b", n3)
n1 = Node("a", n2)

current = n1
while current:
    print(current.value)
    current = current.next
```


5. Why is `collections.deque` preferred over a plain list for implementing a queue?
