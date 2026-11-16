
(defn sum-list-tail [items]
  (loop [l items acc 0]
    (if (empty? l)
      acc
      (recur (rest l) (+ acc (first l))))))


(println (sum-list-tail '(1 2 3 4 5 6 7 8 9 10))) ;=> 10
