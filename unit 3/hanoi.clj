(defn hanoi [n source target extra]
  (when (pos? n)
    (hanoi (dec n) source extra target)
    (println "Move Disk" n "from" source "to" target)
    (hanoi (dec n ) extra target source)))


(hanoi 10 "A" "C" "B")