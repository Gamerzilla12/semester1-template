(defn linear-search [items target]
  (loop [idx 0 remaining items]
    (cond
      (empty? remaining) -1
      (= (first remaining) target) idx
      :else (recur (inc idx) (rest remaining)))))



(defn binary-search [items_list target]
  (let [items (vec items_list)]
    (loop [start 0 end (dec (count items)) number 1]
      (if (> start end)
        -1
        (let [mid (quot (+ start end) 2) mid-val (get items mid)]
          (cond
            (= mid-val target) [mid number]
            (< mid-val target) (recur (inc mid) end (inc number))
            (> mid-val target) (recur start (dec mid) (inc number))))))))


(println (linear-search [10 20 30 40 50] 30))
(println (linear-search [10 20 30 40 50] 99))
(println (linear-search [10 20 30 40 50] 10))

(println (binary-search [10 20 30 40 50] 40))
