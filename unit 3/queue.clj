; Treat a vector as a queue using rest
(def spawn-queue ["goblin" "goblin" "slime" "dragon"])

(defn queue-peek [q] (first q))
(defn queue-pop [q] (rest q))

(println (queue-peek spawn-queue))  ;=> "goblin"
(println (queue-pop spawn-queue))
