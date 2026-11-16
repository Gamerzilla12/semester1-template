import time
def fib(n):
    if n == 0: #case 1
        return 1
    elif n == 1: #case 2
        return 1;
    else:
        return fib(n - 1) + fib(n - 2)

#print(fib(10))

start = time.perf_counter()
print(fib(30))
print(f'took {time.perf_counter() - start:.3f}s')

start = time.perf_counter()
print(fib(35))
print(f'took {time.perf_counter() - start:.3f}s')


def fib_cache(n, cache=None):
    if cache is None:
        cache = {}
    if n in cache:
        return cache[n]
    if n <= 1:
        return n
    result = fib_cache(n - 1, cache) + fib_cache(n - 2, cache)
    cache[n] = result
    return result

start = time.perf_counter()
print(fib_cache(100))
print(f'took {time.perf_counter() - start:.3f}s')
