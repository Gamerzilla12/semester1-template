;; cons cell   [car|cdr]

(def snake (cons "head" (cons "body1" (cons "body2" (cons "tail" '())))))

(defn grow [snake segment]
  (cons segment snake))

(defn move [snake]
  (let [grown (cons "new-head" snake)]
    (loop [ret [] s grown]
      (if (next s)
        (recur (conj ret (first s)) (next s))
        (seq ret)))))

(defn delete-segment [snake value]
  (remove #(= % value) snake))
