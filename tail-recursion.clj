(defn factorial [nn]
  (let [n (bigint nn)]
    (if (<= n 1N)
      1N
      (* n (factorial (- n 1))))))


;(println (factorial 10))

(defn factorial-tail [n]
  (loop [i (bigint n) acc 1N]
    (if (<= i 1)
      acc
      (recur (- i 1) (* acc i)))))

;(println (factorial-tail 10))
;(println (factorial-tail 10000))
;(println (factorial 10000))


;; Fibonacci with recur -- efficient, no naive-recursion blowup
(defn fib-recur [n]
  (loop [a 0 b 1 count n]
    (if (zero? count)
      a
      (recur b (+ a b) (dec count)))))

(println (fib-recur 10))  ;=> 55
(println (fib-recur 50))  ;=> instant, unlike naive recursive fib