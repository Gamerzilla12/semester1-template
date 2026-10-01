import time
import matplotlib.pyplot as plt 

#O(n)
def linear_work(n):
    total = 0
    for i in range(n):
        total += i
    return total

#O(n^2)
def quadratic_work(n):
    total = 0
    for i in range(n):
        for j in range(n):
            total += i
    return total

ns = [500, 1000, 2000, 4000, 8000, 16000]
linear_times = []
quadratic_times = []

for n in ns:
    start = time.perf_counter()
    linear_work(n)
    linear_times.append(time.perf_counter() - start)

    start = time.perf_counter()
    quadratic_work(n)
    quadratic_times.append(time.perf_counter() - start)

plt.plot(ns, linear_times, label="Linear O(n)", marker="o")
plt.plot(ns, quadratic_times, label="Quadratic O(n^2)", marker="o")
plt.xlabel("n")
plt.ylabel("seconds")
plt.title("Runtime Growth: Linear vs. Quadratic")
plt.legend()
plt.savefig("growth_chart.png")
print("Chart saved to growth_chart.png — open it from the Explorer panel.")
