(defn countdown [n] (if (<= n 0) (println "Liftoff!") (do (println n) (countdown (- n 1)))))

(countdown 10)

(defn sum-list [items] 
  (if (empty? items) 
    0 
    (+ (first items) (sum-list (rest items)))))

(defn factorial-tail [n]
  (loop [i (bigint n) acc 1N]
    (if (<= i 1)
      acc
      (recur (- i 1) (* acc i)))))