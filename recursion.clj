(defn countdown [n]
  (if (<= n 0)
    (println "Liftoff!")
    (do 
      (println n)
      (countdown (- n 1)))))

(countdown 10)

(defn sum-list [items]
  (if (empty? items)
    0
    (+ (first items) (sum-list (rest items)))))

(println (sum-list '(1 2 3 4))) ;=> 10

