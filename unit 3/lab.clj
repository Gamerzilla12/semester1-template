(def enemies
  [{:name "goblin" :hp 10 :attack 3}
   {:name "slime" :hp 5 :attack 1}
   {:name "dragon" :hp 100 :attack 25}
   {:name "bat" :hp 7 :attack 2}])

(defn weak? [enemy]
  (< (:hp enemy) 20))

(defn battle-report [enemies]
  {;; TODO 1: get every enemy's name
   :roster nil

   ;; TODO 2: get the names of only the weak enemies (use weak?)
   :weak-enemies nil

   ;; TODO 3: sum up every enemy's :hp
   :total-hp nil

   ;; TODO 4: find the name of the enemy with the highest :attack
   :strongest nil})

(battle-report enemies)
