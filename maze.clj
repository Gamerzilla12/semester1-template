(defn generate-maze [width height]
  (let [grid-w (inc (* width 2))
        grid-h (inc (* height 2))
        walls (atom (vec (repeat grid-h (vec (repeat grid-w true)))))
        visited (atom #{})]
    (letfn [(carve [cx cy]
              (swap! visited conj [cx cy])
              (swap! walls assoc-in [(inc (* cy 2)) (inc (* cx 2))] false)
              (doseq [[dx dy] (shuffle [[0 -1] [0 1] [-1 0] [1 0]])]
                (let [nx (+ cx dx) ny (+ cy dy)]
                  (when (and (<= 0 nx (dec width))
                             (<= 0 ny (dec height))
                             (not (@visited [nx ny])))
                    (swap! walls assoc-in
                           [(+ (inc (* cy 2)) dy) (+ (inc (* cx 2)) dx)]
                           false)
                    (carve nx ny)))))]
      (carve 0 0))
    @walls))

(defn print-maze [grid]
  (doseq [row grid]
    (println (apply str (map #(if % "#" ".") row)))))

(print-maze (generate-maze 12 5))