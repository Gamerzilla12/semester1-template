#1 What is the time complexity of the following function?
# A. O(1)   B. O(n)  C. O(Log n)  D. O(n²)

def mystery(items):
    for i in range(len(items)):
        for j in range(len(items)):
            print(items[i], items[j])


#2 True or False: A function that builds a new list the same size as its input has O(n) space complexity.

#3 Why is it misleading to compare Python's exact runtime in seconds to Clojure's exact runtime in milliseconds and conclude one language is "faster"? 
  
#4 Which grows faster as n increases: O(n log n) or O(n²)?

#5 Short answer: Describe a real-world scenario where early-exit search meaningfully saves time compared to always scanning the full list.

#6 Extra Credit: Explain the how a binary search algorithm works
