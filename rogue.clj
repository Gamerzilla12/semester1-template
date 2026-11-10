;; =============================================================================
;;  rogue.clj -- a teaching clone of the original "Rogue" (1980), in Clojure
;; =============================================================================
;;
;;  HOW TO RUN (needs Clojure 1.11 or newer, and a terminal at least 80x24):
;;
;;      clojure -M rogue.clj
;;
;;  The only dependencies are Clojure itself and the standard Java library.
;;  The terminal is controlled with plain ANSI escape codes plus the Unix
;;  `stty` command, so it works in any traditional Linux terminal.
;;
;;  WHAT THIS PROGRAM TEACHES
;;  -------------------------
;;  1. IMMUTABLE STATE.  The whole game is ONE map (`state`).  Every action is
;;     a function  (state, input) -> new-state.  Nothing is ever mutated.
;;  2. DATA-DRIVEN DESIGN.  Monsters, weapons, armor and so on are plain data
;;     tables (maps and vectors), not classes.
;;  3. PURE vs. IMPURE CODE.  Everything above the "TERMINAL I/O" section is
;;     pure: it can be tested at the REPL without a terminal.  Only the last
;;     section touches the keyboard and the screen.
;;  4. A MODE-BASED STATE MACHINE.  Prompts like "Quaff which potion?" are
;;     just a `:mode` value inside the state; the next keypress is
;;     interpreted according to that mode.
;;  5. PROCEDURAL GENERATION.  Each dungeon level is built from a 3x3 grid of
;;     rooms joined by a random spanning tree of corridors.
;;
;;  THE STATE MAP (the single source of truth)
;;  ------------------------------------------
;;    :depth      current dungeon level (1..26)
;;    :turn       number of turns taken
;;    :tiles      vector of rows; each row is a vector of characters
;;    :rooms      vector of room maps {:x :y :w :h :cell}
;;    :seen       set of [x y] positions the player has discovered
;;    :items      map  [x y] -> item on the floor
;;    :monsters   vector of monster maps {:glyph :pos :hp :awake?}
;;    :player     map of player stats and position
;;    :inv        vector of items in the pack (letter a = index 0, etc.)
;;    :known      set of potion/scroll types the player has identified
;;    :appearances random potion colors / scroll titles for this game
;;    :msgs       messages produced during the current turn
;;    :mode       nil, or a prompt waiting for a keypress (see handle-key)
;;    :overlay    nil, or lines of text drawn on top of the map
;;    :dead / :won  set when the game is over
;; =============================================================================

(ns rogue
  (:require [clojure.string :as str])
  (:import [java.lang ProcessBuilder$Redirect]))


;; =============================================================================
;;  1. CONSTANTS
;; =============================================================================

(def ESC "\u001b")                 ; the ANSI escape character

(def map-w 80)                     ; map is 80 columns wide ...
(def map-h 21)                     ; ... and 21 rows tall (plus a message line
                                   ;     on top and a status line on the bottom)
(def cell-w 26)                    ; the map is split into a 3x3 grid of cells;
(def cell-h 7)                     ;     each cell holds exactly one room
(def max-depth 26)                 ; the Amulet of Yendor lives on this level

(def walkable?
  "Tiles a creature may stand on: floor, corridor, door and stairs."
  #{\. \# \+ \%})

(def dirs
  "Movement keys (the vi keys, as in the original) -> [dx dy]."
  {\h [-1 0] \j [0 1] \k [0 -1] \l [1 0]
   \y [-1 -1] \u [1 -1] \b [-1 1] \n [1 1]})

(def run-dirs
  "Shift + a movement key means 'run'.  Built from `dirs` so we never
  have to type the same table twice:  \\H -> [-1 0], \\J -> [0 1], ..."
  (into {} (for [[k d] dirs] [(first (str/upper-case (str k))) d])))


;; =============================================================================
;;  2. RANDOMNESS AND DICE
;; =============================================================================

(defn rnd
  "Random integer from 1 to n inclusive (a die with n sides)."
  [n]
  (inc (rand-int n)))

(defn roll
  "Roll dice written the way role-playing games write them: \"2d4\" means
  roll two four-sided dice and add them up."
  [spec]
  (let [[n sides] (map #(Integer/parseInt %) (str/split spec #"d"))]
    (reduce + (repeatedly n #(rnd sides)))))


;; =============================================================================
;;  3. STATIC DATA TABLES
;; =============================================================================
;;  In Clojure we describe game objects as DATA.  To add a new monster you add
;;  one line to a map; no new code is needed.  (Exercise: add one!)

(def monster-types
  "Keyed by the letter that is drawn on screen.
     :lvl     monster level (also its hit dice: hp = lvl d8)
     :dmg     damage dice          :xp   experience for killing it
     :ac      armor class (LOWER is harder to hit)
     :special optional special behaviour, handled in the monster code below"
  {\A {:name "aquator"       :lvl 5  :dmg "1d3"  :xp 20   :ac 2  :special :rust}
   \B {:name "bat"           :lvl 1  :dmg "1d2"  :xp 1    :ac 3  :special :erratic}
   \C {:name "centaur"       :lvl 4  :dmg "1d5"  :xp 17   :ac 4}
   \D {:name "dragon"        :lvl 10 :dmg "3d10" :xp 5000 :ac -1}
   \E {:name "emu"           :lvl 1  :dmg "1d2"  :xp 2    :ac 7}
   \F {:name "venus flytrap" :lvl 8  :dmg "2d3"  :xp 80   :ac 3  :special :stationary}
   \G {:name "griffin"       :lvl 13 :dmg "4d3"  :xp 2000 :ac 2}
   \H {:name "hobgoblin"     :lvl 1  :dmg "1d8"  :xp 3    :ac 5}
   \I {:name "ice monster"   :lvl 1  :dmg "1d2"  :xp 5    :ac 9}
   \J {:name "jabberwock"    :lvl 15 :dmg "2d12" :xp 3000 :ac 6}
   \K {:name "kestrel"       :lvl 1  :dmg "1d4"  :xp 1    :ac 7}
   \L {:name "leprechaun"    :lvl 3  :dmg "1d2"  :xp 10   :ac 8  :special :steal-gold}
   \M {:name "medusa"        :lvl 8  :dmg "3d4"  :xp 200  :ac 2}
   \N {:name "nymph"         :lvl 3  :dmg "1d2"  :xp 37   :ac 9  :special :steal-item}
   \O {:name "orc"           :lvl 1  :dmg "1d6"  :xp 5    :ac 6}
   \P {:name "phantom"       :lvl 8  :dmg "4d4"  :xp 120  :ac 3}
   \Q {:name "quasit"        :lvl 3  :dmg "1d4"  :xp 15   :ac 2}
   \R {:name "rattlesnake"   :lvl 2  :dmg "1d6"  :xp 9    :ac 3}
   \S {:name "snake"         :lvl 1  :dmg "1d3"  :xp 2    :ac 5}
   \T {:name "troll"         :lvl 6  :dmg "2d6"  :xp 120  :ac 4}
   \U {:name "ur-vile"       :lvl 7  :dmg "1d9"  :xp 190  :ac -2}
   \V {:name "vampire"       :lvl 8  :dmg "1d10" :xp 350  :ac 2}
   \W {:name "wraith"        :lvl 5  :dmg "1d6"  :xp 55   :ac 4}
   \X {:name "xeroc"         :lvl 7  :dmg "4d4"  :xp 100  :ac 7}
   \Y {:name "yeti"          :lvl 4  :dmg "1d6"  :xp 50   :ac 6}
   \Z {:name "zombie"        :lvl 2  :dmg "1d8"  :xp 6    :ac 8}})

(def monster-order
  "Weakest to strongest.  Deeper levels pick letters from further along."
  "KEBSHIROZLCQANYFTWPXUMVGJD")

(def potion-types [:healing :extra-healing :strength :poison])
(def scroll-types [:enchant-weapon :enchant-armor :teleportation :magic-mapping])

(def potion-colors ["red" "blue" "green" "yellow" "purple" "orange" "pink" "clear"])
(def scroll-titles ["ZELGO MER" "XYZZY" "FOOBIE BLETCH" "KLAATU" "NIKTO"
                    "ELBIB YLOH" "VE FORBRYDERNE" "PRATYAVAYAH"])

(def weapon-types
  [{:name "dagger"           :dmg "1d6"}
   {:name "spear"            :dmg "1d8"}
   {:name "mace"             :dmg "2d4"}
   {:name "long sword"       :dmg "1d12"}
   {:name "two-handed sword" :dmg "3d6"}])

(def armor-types
  "Armor class: LOWER is better, just like the original game."
  [{:name "leather armor" :ac 8} {:name "ring mail"   :ac 7}
   {:name "scale mail"    :ac 6} {:name "chain mail"  :ac 5}
   {:name "banded mail"   :ac 4} {:name "splint mail" :ac 3}
   {:name "plate mail"    :ac 2}])

(def item-glyphs
  {:potion \! :scroll \? :food \: :weapon \) :armor \] :gold \* :amulet \,})

(def stackable? #{:potion :scroll :food})

(def xp-levels
  "Experience needed to reach level 2, 3, 4, ..."
  [10 20 40 80 160 320 640 1300 2600 5200 13000 26000 50000 100000
   200000 400000 800000 2000000 4000000 8000000])


;; =============================================================================
;;  4. SMALL HELPER FUNCTIONS (pure, reusable)
;; =============================================================================

(defn msg
  "Add a message to this turn's message list.  Returns the new state.
  Notice the pattern: every function takes `state` first and returns state,
  so they chain nicely with the threading macro `->`."
  [state & parts]
  (update state :msgs conj (apply str parts)))

(defn remove-at
  "Return vector v without the element at index i."
  [v i]
  (vec (concat (subvec v 0 i) (subvec v (inc i)))))

(defn letter [i] (char (+ (int \a) i)))           ; 0 -> \a, 1 -> \b, ...

(defn a-an [word]
  (str (if (contains? #{\a \e \i \o \u} (first word)) "an " "a ") word))

(defn count-noun
  "(count-noun 1 \"potion\") -> \"a potion\";  (count-noun 3 \"potion\") -> \"3 potions\""
  [n noun]
  (if (= n 1) (a-an noun) (str n " " noun "s")))

(defn cheb
  "Chebyshev distance: the number of king-moves between two squares.
  Two squares are adjacent (even diagonally) when this equals 1."
  [[x1 y1] [x2 y2]]
  (max (abs (- x1 x2)) (abs (- y1 y2))))

(defn dist2
  "Squared straight-line distance (no square root needed just to compare)."
  [[x1 y1] [x2 y2]]
  (+ (* (- x1 x2) (- x1 x2)) (* (- y1 y2) (- y1 y2))))


;; =============================================================================
;;  5. LEVEL GENERATION
;; =============================================================================
;;  Plan:
;;    a. Divide the 80x21 map into a 3x3 grid of cells.
;;    b. Put one randomly sized room in each cell.
;;    c. Connect the rooms with corridors.  We pick the connections with a
;;       random depth-first walk over the grid (a "spanning tree": every room
;;       reachable, no pointless loops) and then add two extra links so the
;;       level has a few loops to run around.
;;    d. Drop stairs, items, gold and monsters into the rooms.
;;
;;  Tile characters:  space = solid rock   - and | = walls   . = floor
;;                    + = door   # = corridor   % = stairs

(def grid-cells (for [cy (range 3) cx (range 3)] [cx cy]))

(defn grid-neighbors
  "The cells directly left/right/above/below a cell, staying on the 3x3 grid."
  [[cx cy]]
  (filter (fn [[x y]] (and (<= 0 x 2) (<= 0 y 2)))
          [[(inc cx) cy] [(dec cx) cy] [cx (inc cy)] [cx (dec cy)]]))

(defn blank-tiles
  "A map full of solid rock."
  []
  (vec (repeat map-h (vec (repeat map-w \space)))))

(defn make-room
  "Make a random room somewhere inside grid cell [cx cy].
  A room is {:x :y :w :h}: the top-left corner of the FLOOR area and its
  size.  The walls are one tile further out on every side.  The numbers below
  guarantee a gap of rock between neighbouring rooms so corridors fit."
  [cx cy]
  (let [w  (+ 4 (rand-int 19))                         ; floor width  4..22
        h  (+ 2 (rand-int 3))                          ; floor height 2..4
        ow (+ w 2)                                     ; outer size (with walls)
        oh (+ h 2)
        ox (+ (* cx cell-w) 1 (rand-int (- 25 ow)))    ; outer top-left corner
        oy (+ (* cy cell-h) 1 (rand-int (- 7 oh)))]
    {:x (inc ox) :y (inc oy) :w w :h h :cell [cx cy]}))

(defn random-floor
  "A random floor position inside a room."
  [{:keys [x y w h]}]
  [(+ x (rand-int w)) (+ y (rand-int h))])

(defn draw-room
  "Return tiles with the room's walls and floor drawn in."
  [tiles {:keys [x y w h]}]
  (reduce (fn [t [tx ty]]
            (assoc-in t [ty tx]
                      (cond (or (= ty (dec y)) (= ty (+ y h))) \-
                            (or (= tx (dec x)) (= tx (+ x w))) \|
                            :else \.)))
          tiles
          (for [ty (range (dec y) (+ y h 1))
                tx (range (dec x) (+ x w 1))]
            [tx ty])))

(defn line
  "All [x y] points on an axis-aligned line (inclusive).  One of the two
  coordinates always matches, so the `for` yields a straight run of tiles."
  [[x1 y1] [x2 y2]]
  (for [x (range (min x1 x2) (inc (max x1 x2)))
        y (range (min y1 y2) (inc (max y1 y2)))]
    [x y]))

(defn carve
  "Turn rock at the given points into corridor.  Existing walls, floors and
  doors are left alone."
  [tiles points]
  (reduce (fn [t [x y]]
            (if (= \space (get-in t [y x])) (assoc-in t [y x] \#) t))
          tiles points))

(defn connect
  "Join room a to room b with a door in each and an L/Z-shaped corridor.
  Room a must be to the left of, or above, room b."
  [tiles a b]
  (if (= (second (:cell a)) (second (:cell b)))
    ;; --- same row of the grid: door on a's right wall, b's left wall -------
    (let [ay (+ (:y a) (rand-int (:h a)))
          by (+ (:y b) (rand-int (:h b)))
          adoor [(+ (:x a) (:w a)) ay]
          bdoor [(dec (:x b)) by]
          sx (inc (first adoor))                       ; first corridor tile
          ex (dec (first bdoor))                       ; last corridor tile
          mx (+ sx (rand-int (inc (- ex sx))))]        ; where the corridor bends
      (-> tiles
          (carve (concat (line [sx ay] [mx ay])
                         (line [mx ay] [mx by])
                         (line [mx by] [ex by])))
          (assoc-in [(second adoor) (first adoor)] \+)
          (assoc-in [(second bdoor) (first bdoor)] \+)))
    ;; --- same column: door on a's bottom wall, b's top wall ----------------
    (let [ax (+ (:x a) (rand-int (:w a)))
          bx (+ (:x b) (rand-int (:w b)))
          adoor [ax (+ (:y a) (:h a))]
          bdoor [bx (dec (:y b))]
          sy (inc (second adoor))
          ey (dec (second bdoor))
          my (+ sy (rand-int (inc (- ey sy))))]
      (-> tiles
          (carve (concat (line [ax sy] [ax my])
                         (line [ax my] [bx my])
                         (line [bx my] [bx ey])))
          (assoc-in [(second adoor) (first adoor)] \+)
          (assoc-in [(second bdoor) (first bdoor)] \+)))))

(defn random-tree-edges
  "A random spanning tree of the 3x3 grid, as a vector of [cell cell] pairs.
  This is a randomized depth-first search: keep a stack of cells, step to a
  random unvisited neighbour when there is one, otherwise back up."
  []
  (let [start [(rand-int 3) (rand-int 3)]]
    (loop [stack [start] visited #{start} edges []]
      (if (empty? stack)
        edges
        (let [here    (peek stack)
              options (remove visited (grid-neighbors here))]
          (if (empty? options)
            (recur (pop stack) visited edges)
            (let [nxt (rand-nth options)]
              (recur (conj stack nxt) (conj visited nxt)
                     (conj edges [here nxt])))))))))

(defn all-grid-edges
  "Every pair of neighbouring cells, each pair listed once."
  []
  (for [c grid-cells
        n (grid-neighbors c)
        :when (neg? (compare c n))]
    [c n]))

(defn random-ench
  "Weapons and armor are occasionally cursed (-1) or blessed (+1)."
  []
  (let [r (rnd 100)]
    (cond (<= r 10) -1
          (<= r 25) 1
          :else 0)))

(defn random-item
  "Roll for a random floor item.  The numbers are percentages."
  []
  (let [r (rnd 100)]
    (cond
      (<= r 26) {:kind :potion
                 :type (rand-nth [:healing :healing :extra-healing :strength :poison])}
      (<= r 62) {:kind :scroll
                 :type (rand-nth [:enchant-weapon :enchant-armor :enchant-armor
                                  :teleportation :magic-mapping])}
      (<= r 78) {:kind :food}
      (<= r 86) (assoc (rand-nth weapon-types) :kind :weapon :ench (random-ench))
      :else     (assoc (rand-nth armor-types)  :kind :armor  :ench (random-ench)))))

(defn make-level
  "Build a brand new level for the given depth.  Returns a map holding
  :tiles :rooms :items :monsters and :start (where the player appears)."
  [depth]
  (let [room-map (into {} (for [[cx cy :as c] grid-cells] [c (make-room cx cy)]))
        tree     (random-tree-edges)
        have     (set (map set tree))
        extra    (take 2 (shuffle (remove #(have (set %)) (all-grid-edges))))
        tiles    (reduce draw-room (blank-tiles) (vals room-map))
        tiles    (reduce (fn [t [c1 c2]]
                           (let [[a b] (sort [c1 c2])]   ; left/top room first
                             (connect t (room-map a) (room-map b))))
                         tiles (concat tree extra))
        rooms    (vec (vals room-map))
        start-rm (rand-nth rooms)
        stair-rm (rand-nth (remove #{start-rm} rooms))
        start    (random-floor start-rm)
        stairs   (random-floor stair-rm)
        tiles    (assoc-in tiles [(second stairs) (first stairs)] \%)
        taken    #{start stairs}
        free-pos (fn [] (let [p (random-floor (rand-nth rooms))]
                          (if (taken p) (recur) p)))
        items    (into {}
                       (concat
                        (for [_ (range (+ 3 (rand-int 4)))]
                          [(free-pos) (random-item)])
                        (for [_ rooms :when (< (rand) 0.5)]
                          [(free-pos) {:kind :gold
                                       :amount (+ 2 (rnd (+ 50 (* 10 depth))))}])
                        (when (= depth max-depth)
                          [[(free-pos) {:kind :amulet}]])))
        far-pos  (fn [] (let [p (free-pos)]
                          (if (< (cheb p start) 7) (recur) p)))
        n-mons   (min 12 (+ 3 (rand-int 3) (quot depth 4)))
        monsters (reduce
                  (fn [ms _]
                    (let [p     (far-pos)
                          glyph (nth monster-order
                                     (-> (+ depth (rand-int 10) -6) (max 0) (min 25)))]
                      (if (some #(= p (:pos %)) ms)
                        ms
                        (conj ms {:glyph glyph :pos p
                                  :hp (roll (str (:lvl (monster-types glyph)) "d8"))
                                  :awake? (< (rand) 0.25)}))))
                  [] (range n-mons))]
    {:tiles tiles :rooms rooms :items items :monsters monsters :start start}))


;; =============================================================================
;;  6. QUERIES ABOUT THE STATE (read-only helpers)
;; =============================================================================

(defn tile-at
  "The tile character at [x y]; solid rock if off the map."
  [state [x y]]
  (get-in (:tiles state) [y x] \space))

(defn monster-index-at
  "Index into (:monsters state) of the live monster standing at pos, or nil."
  [state pos]
  (first (keep-indexed (fn [i m] (when (and (= (:pos m) pos) (not (:gone m))) i))
                       (:monsters state))))

(defn can-step?
  "May a creature at `from` move by [dx dy]?  The destination must be
  walkable, and -- like the original game -- nobody may move diagonally into
  or out of a doorway."
  [state from [dx dy]]
  (let [to        [(+ (first from) dx) (+ (second from) dy)]
        diagonal? (and (not= dx 0) (not= dy 0))]
    (and (walkable? (tile-at state to))
         (not (and diagonal?
                   (or (= \+ (tile-at state from))
                       (= \+ (tile-at state to))))))))

(defn room-containing
  "The room whose floor OR walls include pos (so a doorway counts), or nil."
  [rooms [px py]]
  (first (filter (fn [{:keys [x y w h]}]
                   (and (<= (dec x) px (+ x w)) (<= (dec y) py (+ y h))))
                 rooms)))

(defn visible-monster?
  "Can the player see this monster?  Yes if it is adjacent, or if the player
  and the monster are in the same (lit) room."
  [state m]
  (let [ppos  (get-in state [:player :pos])
        proom (room-containing (:rooms state) ppos)]
    (or (<= (cheb (:pos m) ppos) 1)
        (and proom (= proom (room-containing (:rooms state) (:pos m)))))))

(defn update-seen
  "Remember what the player can see right now.  Rooms are lit, so stepping
  into one reveals the whole room; in a corridor you only see adjacent tiles."
  [state]
  (let [[px py] (get-in state [:player :pos])
        room    (room-containing (:rooms state) [px py])
        near    (for [dx [-1 0 1] dy [-1 0 1]] [(+ px dx) (+ py dy)])
        lit     (when room
                  (let [{:keys [x y w h]} room]
                    (for [tx (range (dec x) (+ x w 1))
                          ty (range (dec y) (+ y h 1))]
                      [tx ty])))]
    (update state :seen into (concat near lit))))

(defn has-amulet? [state]
  (some #(= :amulet (:kind %)) (:inv state)))

(defn worn-index    [inv] (first (keep-indexed (fn [i it] (when (:worn it) i)) inv)))
(defn wielded-index [inv] (first (keep-indexed (fn [i it] (when (:wielded it) i)) inv)))

(defn player-ac
  "Armor class: 10 with no armor, otherwise the armor's value minus its
  enchantment.  Lower is better."
  [inv]
  (if-let [i (worn-index inv)]
    (- (:ac (inv i)) (:ench (inv i) 0))
    10))

(defn dmg-bonus
  "Strength adds to (or subtracts from) damage."
  [s]
  (cond (>= s 18) 2 (>= s 17) 1 (< s 8) -1 :else 0))


;; =============================================================================
;;  7. ITEMS: NAMES, PACK MANAGEMENT AND EFFECTS
;; =============================================================================

(defn item-name
  "Human-readable description of an item.  Unidentified potions and scrolls
  are described by their random appearance (colour or title)."
  [state item]
  (let [n      (:count item 1)
        known  (:known state)
        words  (fn [k] (str/replace (name k) "-" " "))
        suffix (str (when (:wielded item) " (weapon in hand)")
                    (when (:worn item)    " (being worn)"))]
    (case (:kind item)
      :potion (let [t (:type item)]
                (if (known t)
                  (str (count-noun n "potion") " of " (words t))
                  (count-noun n (str (get-in state [:appearances :potion t]) " potion"))))
      :scroll (let [t (:type item)]
                (if (known t)
                  (str (count-noun n "scroll") " of " (words t))
                  (str (count-noun n "scroll") " titled '"
                       (get-in state [:appearances :scroll t]) "'")))
      :food   (if (= n 1) "a ration of food" (str n " rations of food"))
      :weapon (str (when (:revealed item) (format "%+d " (:ench item 0)))
                   (:name item) " [" (:dmg item) "]" suffix)
      :armor  (str (when (:revealed item) (format "%+d " (:ench item 0)))
                   (:name item)
                   " [" (- (:ac item) (if (:revealed item) (:ench item 0) 0)) "]"
                   suffix)
      :amulet "the Amulet of Yendor"
      "something strange")))

(defn add-to-pack
  "Try to put item in the pack (a vector).  Potions, scrolls and food of the
  same kind stack into one slot.  Returns [new-pack slot-index], or nil if
  the pack is full (26 slots, a-z)."
  [inv item]
  (let [same? (fn [i] (and (stackable? (:kind item))
                           (= (:kind i) (:kind item))
                           (= (:type i) (:type item))))
        idx   (first (keep-indexed (fn [n i] (when (same? i) n)) inv))]
    (cond
      idx                [(update-in inv [idx :count]
                                     (fn [c] (+ (or c 1) (:count item 1))))
                          idx]
      (< (count inv) 26) [(conj inv item) (count inv)]
      :else              nil)))

(defn use-up
  "Remove ONE of the item in slot idx (the whole item if it is not a stack)."
  [inv idx]
  (if (> (:count (inv idx) 1) 1)
    (update-in inv [idx :count] dec)
    (remove-at inv idx)))

(defn pickup
  "Pick up whatever lies under the player.  Gold goes to the purse; anything
  else goes in the pack if there is room."
  [state]
  (let [pos  (get-in state [:player :pos])
        item ((:items state) pos)]
    (cond
      (nil? item) state

      (= :gold (:kind item))
      (-> state
          (update-in [:player :gold] + (:amount item))
          (update :items dissoc pos)
          (msg "You found " (:amount item) " pieces of gold."))

      :else
      (if-let [[inv idx] (add-to-pack (:inv state) item)]
        (-> state
            (assoc :inv inv)
            (update :items dissoc pos)
            (msg "You now have " (letter idx) ") " (item-name state (inv idx))))
        (msg state "Your pack is too full to pick up "
             (item-name state item) ".")))))

(defn heal
  "Restore n hit points.  Healing beyond your maximum raises the maximum by 1."
  [state n text]
  (let [{:keys [hp max-hp]} (:player state)
        new-hp  (+ hp n)
        new-max (if (> new-hp max-hp) (inc max-hp) max-hp)]
    (-> state
        (assoc-in [:player :max-hp] new-max)
        (assoc-in [:player :hp] (min new-hp new-max))
        (msg text))))

(defn quaff [state idx]
  (let [t     (:type ((:inv state) idx))
        state (-> state (update :inv use-up idx) (update :known conj t))]
    (case t
      :healing       (heal state (roll "6d4")  "You begin to feel better.")
      :extra-healing (heal state (roll "10d4") "You begin to feel much better.")
      :strength      (-> state
                         (update-in [:player :str] inc)
                         (update-in [:player :max-str] inc)
                         (msg "You feel stronger.  What bulging muscles!"))
      :poison        (-> state
                         (update-in [:player :str] #(max 3 (dec %)))
                         (msg "You feel very sick now.")))))

(defn read-scroll [state idx]
  (let [t     (:type ((:inv state) idx))
        ;; NB: the scroll is removed first, so slot numbers below are looked
        ;; up in the NEW pack -- removing an item can shift later slots.
        state (-> state (update :inv use-up idx) (update :known conj t))
        inv   (:inv state)]
    (case t
      :enchant-weapon
      (if-let [i (wielded-index inv)]
        (-> state
            (update-in [:inv i :ench] (fnil inc 0))
            (assoc-in  [:inv i :revealed] true)
            (msg "Your " (:name (inv i)) " glows blue for a moment."))
        (msg state "You feel a strange sense of loss."))

      :enchant-armor
      (if-let [i (worn-index inv)]
        (-> state
            (update-in [:inv i :ench] (fnil inc 0))
            (assoc-in  [:inv i :revealed] true)
            (msg "Your " (:name (inv i)) " gleams silver for a moment."))
        (msg state "You feel a strange sense of loss."))

      :teleportation
      (-> state
          (assoc-in [:player :pos] (random-floor (rand-nth (:rooms state))))
          (msg "Your position suddenly changes!")
          pickup)

      :magic-mapping
      (-> state
          (update :seen into
                  (for [y (range map-h) x (range map-w)
                        :when (not= \space (tile-at state [x y]))]
                    [x y]))
          (msg "Oh, now this scroll has a map on it!")))))

(defn eat [state idx]
  (-> state
      (update :inv use-up idx)
      (update-in [:player :food] #(min 2000 (+ % 1100)))
      (msg "Yum, that tasted good.")))

(defn equip
  "Return the pack with `flag` (:wielded or :worn) set on slot idx and
  cleared from every other slot, so only one weapon/armor is in use."
  [inv idx flag]
  (vec (map-indexed (fn [i it]
                      (if (= i idx)
                        (assoc it flag true :revealed true)
                        (dissoc it flag)))
                    inv)))

(defn wield [state idx]
  (let [inv (equip (:inv state) idx :wielded)]
    (-> state (assoc :inv inv) (msg "You are now wielding " (item-name state (inv idx)) "."))))

(defn wear [state idx]
  (let [inv (equip (:inv state) idx :worn)]
    (-> state (assoc :inv inv) (msg "You are now wearing " (item-name state (inv idx)) "."))))

(defn drop-item [state idx]
  (let [pos (get-in state [:player :pos])
        one (-> ((:inv state) idx) (dissoc :wielded :worn) (assoc :count 1))]
    (if (contains? (:items state) pos)
      (msg state "There is already something here.")
      (-> state
          (update :inv use-up idx)
          (update :items assoc pos one)
          (msg "You dropped " (item-name state one) ".")))))


;; =============================================================================
;;  8. COMBAT
;; =============================================================================

(defn swing?
  "Does an attack hit?  This is the formula from the original Rogue source:
  roll a d20, add a bonus, and compare with a target number that gets easier
  as the attacker's level rises and as the defender's armor class falls."
  [attacker-level defender-ac bonus]
  (>= (+ (rnd 20) bonus) (- 20 attacker-level defender-ac)))

(defn gain-xp
  "Add experience; if that crosses a threshold, level up and gain hit points."
  [state n]
  (let [old-lvl (get-in state [:player :level])
        xp      (+ (get-in state [:player :xp]) n)
        new-lvl (inc (count (take-while #(>= xp %) xp-levels)))
        state   (assoc-in state [:player :xp] xp)]
    (if (<= new-lvl old-lvl)
      state
      (-> (reduce (fn [s _]
                    (let [bonus (rnd 10)]
                      (-> s
                          (update-in [:player :max-hp] + bonus)
                          (update-in [:player :hp] + bonus))))
                  state (range (- new-lvl old-lvl)))
          (assoc-in [:player :level] new-lvl)
          (msg "Welcome to level " new-lvl "!")))))

(defn player-attack
  "The player attacks the monster at index idx."
  [state idx]
  (let [m     ((:monsters state) idx)
        t     (monster-types (:glyph m))
        inv   (:inv state)
        w     (when-let [i (wielded-index inv)] (inv i))
        ench  (:ench w 0)
        dice  (if w (:dmg w) "1d4")                       ; bare hands: 1d4
        plvl  (get-in state [:player :level])
        state (assoc-in state [:monsters idx :awake?] true)]
    (if (swing? plvl (:ac t) ench)
      (let [dmg (max 1 (+ (roll dice) ench (dmg-bonus (get-in state [:player :str]))))
            hp  (- (:hp m) dmg)]
        (if (pos? hp)
          (-> state
              (assoc-in [:monsters idx :hp] hp)
              (msg "You hit the " (:name t) "."))
          (-> state
              (update :monsters remove-at idx)
              (msg "You killed the " (:name t) ".")
              (gain-xp (:xp t)))))
      (msg state "You miss the " (:name t) "."))))

(defn apply-special
  "Extra effects a monster has when its attack lands."
  [state idx special]
  (case special
    :steal-gold
    (let [gold (get-in state [:player :gold])]
      (if (pos? gold)
        (-> state
            (update-in [:player :gold] - (min gold (inc (rand-int 50))))
            (assoc-in [:monsters idx :gone] true)
            (msg "Your purse feels lighter.  The leprechaun vanishes!"))
        state))

    :steal-item
    (let [candidates (keep-indexed (fn [i it] (when-not (or (:wielded it) (:worn it)) i))
                                   (:inv state))]
      (if (seq candidates)
        (let [i (rand-nth candidates)]
          (-> state
              (update :inv remove-at i)
              (assoc-in [:monsters idx :gone] true)
              (msg "The nymph stole your " (item-name state ((:inv state) i)) "!")))
        state))

    :rust
    (if-let [i (worn-index (:inv state))]
      (-> state
          (update-in [:inv i :ench] #(max -3 (dec (or % 0))))
          (msg "Your armor weakens!"))
      state)

    state))                                  ; default: no special effect

(defn monster-attack
  "The monster at index idx attacks the player."
  [state idx]
  (let [m  ((:monsters state) idx)
        t  (monster-types (:glyph m))
        nm (:name t)]
    (if-not (swing? (:lvl t) (player-ac (:inv state)) 0)
      (msg state "The " nm " misses you.")
      (let [hp (- (get-in state [:player :hp]) (roll (:dmg t)))
            s  (-> state (assoc-in [:player :hp] hp) (msg "The " nm " hits you."))]
        (if (<= hp 0)
          (assoc s :dead (str "killed by " (a-an nm)))
          (apply-special s idx (:special t)))))))


;; =============================================================================
;;  9. MONSTER ARTIFICIAL INTELLIGENCE
;; =============================================================================
;;  Monsters follow very simple rules -- which is exactly how the original
;;  worked:  sleep until the player gets close;  attack if adjacent;  else
;;  take the step that gets closest to the player.

(defn delta
  "The [dx dy] step (each -1, 0 or 1) pointing from `from` toward `to`."
  [[x1 y1] [x2 y2]]
  [(compare x2 x1) (compare y2 y1)])

(defn chase
  "Move monster idx one step toward the player (greedy, no path-finding)."
  [state idx]
  (let [m       ((:monsters state) idx)
        t       (monster-types (:glyph m))
        pos     (:pos m)
        ppos    (get-in state [:player :pos])
        options (for [d (vals dirs)
                      :when (can-step? state pos d)
                      :let  [to (mapv + pos d)]
                      :when (and (nil? (monster-index-at state to))
                                 (not= to ppos))]
                  to)
        best    (when (seq options) (apply min-key #(dist2 % ppos) options))
        ;; bats flutter randomly; anyone stuck behind a wall sometimes wanders
        wander? (or (and (= :erratic (:special t)) (< (rand) 0.5))
                    (and best (>= (dist2 best ppos) (dist2 pos ppos)) (< (rand) 0.3)))
        target  (cond (empty? options) nil
                      wander?          (rand-nth options)
                      (< (dist2 best ppos) (dist2 pos ppos)) best)]
    (if target
      (assoc-in state [:monsters idx :pos] target)
      state)))

(defn monster-turn
  "Let monster idx act.  Returns the new state."
  [state idx]
  (let [m    ((:monsters state) idx)
        t    (monster-types (:glyph m))
        ppos (get-in state [:player :pos])
        d    (cheb (:pos m) ppos)]
    (cond
      (:gone m)         state
      (not (:awake? m)) (if (and (<= d 6) (< (rand) 0.3))        ; may wake up
                          (assoc-in state [:monsters idx :awake?] true)
                          state)
      (and (= d 1) (can-step? state (:pos m) (delta (:pos m) ppos)))
      (monster-attack state idx)
      (= :stationary (:special t)) state
      (> d 20)          state
      :else             (chase state idx))))

(defn move-monsters
  "Every monster takes a turn, in order.  Stops early if the player dies.
  Monsters that stole something are flagged :gone and swept away at the end
  (we never delete mid-loop, because that would shift the indexes)."
  [state]
  (-> (reduce (fn [s i] (if (:dead s) (reduced s) (monster-turn s i)))
              state
              (range (count (:monsters state))))
      (update :monsters #(vec (remove :gone %)))))


;; =============================================================================
;;  10. TURNS, HUNGER, REGENERATION
;; =============================================================================

(defn hunger-status [food]
  (cond (<= food 50)  "Faint"
        (<= food 150) "Weak"
        (<= food 300) "Hungry"
        :else         nil))

(defn tick-hunger [state]
  (let [before (get-in state [:player :food])
        after  (dec before)
        s      (assoc-in state [:player :food] after)]
    (cond
      (<= after 0) (assoc s :dead "starved to death")
      (not= (hunger-status before) (hunger-status after))
      (msg s (case (hunger-status after)
               "Hungry" "You are starting to get hungry."
               "Weak"   "You feel weak from lack of food."
               "Faint"  "You feel faint from lack of food."))
      :else s)))

(defn regen
  "Slowly heal; higher-level characters heal faster."
  [state]
  (let [{:keys [hp max-hp level]} (:player state)]
    (if (and (< hp max-hp)
             (zero? (mod (:turn state) (max 3 (- 21 (* 2 level))))))
      (update-in state [:player :hp] inc)
      state)))

(defn end-turn
  "Everything that happens after the player uses up a turn."
  [state]
  (let [s (move-monsters state)]
    (if (:dead s)
      s
      (-> s tick-hunger regen update-seen (update :turn inc)))))


;; =============================================================================
;;  11. PLAYER ACTIONS
;; =============================================================================

(defn move-player
  "Try to move (or attack) in direction [dx dy].  Bumping into a wall costs
  no time, so the state is returned unchanged."
  [state [dx dy]]
  (let [pos (get-in state [:player :pos])
        to  [(+ (first pos) dx) (+ (second pos) dy)]]
    (if-not (can-step? state pos [dx dy])
      state
      (if-let [idx (monster-index-at state to)]
        (end-turn (player-attack state idx))                  ; a monster is there
        (end-turn (-> state (assoc-in [:player :pos] to) pickup))))))

(defn run-player
  "Shift+direction: keep walking until something interesting happens --
  a message, a wall, a door or the stairs, or a monster nearby."
  [state dir]
  (loop [s state steps 0]
    (let [before (get-in s [:player :pos])
          s2     (move-player s dir)
          after  (get-in s2 [:player :pos])
          ppos   after
          danger? (some (fn [m] (and (visible-monster? s2 m)
                                     (or (:awake? m) (<= (cheb (:pos m) ppos) 2))))
                        (:monsters s2))]
      (if (or (= before after)
              (:dead s2)
              (seq (:msgs s2))
              danger?
              (#{\+ \%} (tile-at s2 after))
              (>= steps 60))
        s2
        (recur s2 (inc steps))))))

(defn change-level
  "Replace the current level with a freshly generated one."
  [state depth]
  (let [level (make-level depth)]
    (-> state
        (merge level)
        (assoc :depth depth :seen #{} :mode nil)
        (assoc-in [:player :pos] (:start level))
        update-seen)))

(defn on-stairs? [state]
  (= \% (tile-at state (get-in state [:player :pos]))))

(defn descend [state]
  (cond
    (not (on-stairs? state))      (msg state "I see no way down.")
    (>= (:depth state) max-depth) (msg state "There is nowhere deeper to go!")
    :else (-> state
              (change-level (inc (:depth state)))
              (msg "You descend to level " (inc (:depth state)) "."))))

(defn ascend
  "Going up is only really meaningful once you hold the Amulet: climb out of
  level 1 with it and you win.  (Simplification: the original remembers
  levels; we generate a fresh one each time.)"
  [state]
  (cond
    (not (on-stairs? state)) (msg state "I see no way up.")
    (= 1 (:depth state))     (if (has-amulet? state)
                               (assoc state :won true)
                               (msg state "A mysterious force prevents you from leaving."))
    :else (-> state
              (change-level (dec (:depth state)))
              (msg "You climb to level " (dec (:depth state)) "."))))

(defn take-off [state]
  (if-let [i (worn-index (:inv state))]
    (end-turn (-> state
                  (update-in [:inv i] dissoc :worn)
                  (msg "You are no longer wearing your armor.")))
    (msg state "You aren't wearing any armor.")))


;; =============================================================================
;;  12. KEY HANDLING (a tiny state machine)
;; =============================================================================
;;  Normally a key is a COMMAND.  But 'q' (quaff) needs a second key to say
;;  WHICH potion.  We handle this by storing a :mode in the state:
;;      {:type :select :action :quaff :prompt "Quaff which potion?"}
;;  The next keypress is then routed to `handle-mode-key` instead.

(def action-kinds
  "Which kind of item each action works on (drop works on anything)."
  {:quaff :potion :read :scroll :eat :food :wield :weapon :wear :armor})

(defn apply-selection
  "The player chose pack slot idx for an action like :quaff or :drop."
  [state action idx]
  (let [item (get-in state [:inv idx])
        need (action-kinds action)]
    (cond
      (nil? item)
      (msg state "There is no such item.")
      (and need (not= need (:kind item)))
      (msg state "You can't " (name action) " that.")
      :else
      (end-turn
       (case action
         :quaff (quaff state idx)
         :read  (read-scroll state idx)
         :eat   (eat state idx)
         :wield (wield state idx)
         :wear  (wear state idx)
         :drop  (drop-item state idx))))))

(defn handle-mode-key [state mode key]
  (case (:type mode)
    :select  (if (and (char? key) (<= (int \a) (int key) (int \z)))
               (apply-selection state (:action mode) (- (int key) (int \a)))
               state)                                  ; any other key cancels
    :confirm (if (= key \y)
               (assoc state :dead "quit")
               state)))

(defn inventory-lines [state]
  (concat ["Inventory:" ""]
          (if (empty? (:inv state))
            ["  (nothing)"]
            (map-indexed (fn [i item] (str "  " (letter i) ") " (item-name state item)))
                         (:inv state)))
          ["" "(press any key)"]))

(def help-lines
  ["ROGUE -- commands"
   ""
   "  h j k l          move west/south/north/east"
   "  y u b n          move diagonally  (arrow keys work too)"
   "  H J K L Y U B N  run in that direction"
   "  . or s           rest one turn"
   "  > and <          use stairs  (< out of level 1 with the Amulet wins)"
   "  i                show your pack"
   "  q  quaff a potion        r  read a scroll"
   "  e  eat food              w  wield a weapon"
   "  W  wear armor            T  take armor off"
   "  d  drop an item          ,  pick up (gold and items are picked up"
   "                              automatically when you walk over them)"
   "  Q  quit                  ?  this help"
   ""
   "  @ you   % stairs   + door   # corridor   . floor"
   "  ! potion  ? scroll  : food  ) weapon  ] armor  * gold"
   "  Letters A-Z are monsters.  Walk into one to attack it."
   ""
   "(press any key)"])

(defn start-select [state action prompt]
  (if (empty? (:inv state))
    (msg state "You have nothing in your pack.")
    (assoc state :mode {:type :select :action action :prompt prompt})))

(defn handle-command
  "Interpret a key pressed in normal mode."
  [state key]
  (cond
    (= key :eof)     (assoc state :dead "quit")
    (dirs key)       (move-player state (dirs key))
    (run-dirs key)   (run-player state (run-dirs key))
    :else
    (case key
      (\. \s) (end-turn state)
      \,      (end-turn (pickup state))
      \i      (assoc state :overlay (inventory-lines state))
      \?      (assoc state :overlay help-lines)
      \q      (start-select state :quaff "Quaff which potion?  (ESC cancels)")
      \r      (start-select state :read  "Read which scroll?  (ESC cancels)")
      \e      (start-select state :eat   "Eat what?  (ESC cancels)")
      \w      (start-select state :wield "Wield what?  (ESC cancels)")
      \W      (start-select state :wear  "Wear what?  (ESC cancels)")
      \d      (start-select state :drop  "Drop what?  (ESC cancels)")
      \T      (take-off state)
      \>      (descend state)
      \<      (ascend state)
      \Q      (assoc state :mode {:type :confirm :prompt "Really quit?  (y/n)"})
      (msg state "Unknown command.  Press ? for help."))))

(defn handle-key
  "THE heart of the game logic:  (state, key) -> new state.
  It is completely pure, so you can call it from a REPL or a unit test."
  [state key]
  (let [state (assoc state :msgs [])]
    (if-let [mode (:mode state)]
      (handle-mode-key (assoc state :mode nil) mode key)
      (handle-command state key))))


;; =============================================================================
;;  13. NEW GAME
;; =============================================================================

(defn new-game []
  (let [level (make-level 1)]
    (-> {:depth 1
         :turn 0
         :msgs ["Hello adventurer, welcome to the Dungeons of Doom!  (Press ? for help)"]
         :mode nil
         :known #{}
         ;; Each game, potion colours and scroll titles are shuffled, so the
         ;; player must discover what each one does by trying it.
         :appearances {:potion (zipmap potion-types (shuffle potion-colors))
                       :scroll (zipmap scroll-types (shuffle scroll-titles))}
         :inv [{:kind :food :count 1}
               {:kind :weapon :name "mace" :dmg "2d4" :ench 1
                :wielded true :revealed true}
               {:kind :armor :name "ring mail" :ac 7 :ench 1
                :worn true :revealed true}]
         :player {:pos (:start level)
                  :hp 12 :max-hp 12 :str 16 :max-str 16
                  :level 1 :xp 0 :gold 0 :food 1300}
         :seen #{}}
        (merge level)
        update-seen)))


;; =============================================================================
;;  14. TERMINAL I/O  (the only impure part of the program)
;; =============================================================================
;;  Everything below talks to the real terminal.

(defn sh!
  "Run a shell command and return its trimmed standard output.  We use this
  for `stty`, which switches the terminal between 'line mode' (the default:
  you must press Enter) and 'character mode' (we see every key instantly)."
  [cmd]
  (let [pb (doto (ProcessBuilder. ["sh" "-c" cmd])
             (.redirectError ProcessBuilder$Redirect/INHERIT))
        proc (.start pb)
        out  (slurp (.getInputStream proc))]
    (.waitFor proc)
    (str/trim out)))

(defn read-key
  "Block until a key is pressed.  Returns a character, :escape, :eof, or
  :unknown.  Arrow keys arrive as the three bytes ESC [ A..D; we translate
  them to the equivalent vi keys."
  []
  (let [c (.read System/in)]
    (cond
      (= c -1) :eof
      (= c 27) (if (pos? (.available System/in))
                 (if (= 91 (.read System/in))                  ; 91 is '['
                   (case (char (.read System/in))
                     \A \k, \B \j, \C \l, \D \h
                     :unknown)
                   :unknown)
                 :escape)
      :else (char c))))

(defn status-line [state]
  (let [{:keys [hp max-hp str max-str level xp gold food]} (:player state)]
    (format "Level: %d  Gold: %-5d Hp: %d(%d)  Str: %d(%d)  Arm: %d  Exp: %d/%d  %s"
            (:depth state) gold hp max-hp str max-str
            (player-ac (:inv state)) level xp
            (or (hunger-status food) ""))))

(defn render-map-rows
  "Build the 21 rows of the map as strings.  Drawing priority, highest first:
  the player, visible monsters, remembered items, remembered terrain."
  [state]
  (let [{:keys [tiles seen items monsters]} state
        ppos   (get-in state [:player :pos])
        mglyph (into {} (for [m monsters :when (visible-monster? state m)]
                          [(:pos m) (:glyph m)]))]
    (for [y (range map-h)]
      (apply str
             (for [x (range map-w)
                   :let [p [x y]]]
               (cond (= p ppos)        \@
                     (mglyph p)        (mglyph p)
                     (not (seen p))    \space
                     (items p)         (item-glyphs (:kind (items p)))
                     :else             (get-in tiles [y x])))))))

(defn overlay-rows
  "Draw lines of text over the right-hand part of the map."
  [rows lines]
  (map-indexed (fn [i row]
                 (if-let [l (nth lines i nil)]
                   (str (subs row 0 26)
                        (format "%-54s" (subs l 0 (min 54 (count l)))))
                   row))
               rows))

(defn frame
  "The complete screen as one string: message line, map, status line.
  `ESC[H` moves the cursor home; `ESC[2K` erases a line before we draw it."
  [state top]
  (let [rows (cond-> (render-map-rows state)
               (:overlay state) (overlay-rows (:overlay state)))
        clip (fn [s n] (subs s 0 (min n (count s))))]
    (str ESC "[H"
         (str/join "\n"
                   (concat [(str ESC "[2K" (clip top 79))]
                           (map #(str ESC "[2K" %) rows)
                           [(str ESC "[2K" (status-line state))])))))

(defn draw! [state top]
  (print (frame state top))
  (flush))

(defn paginate
  "Pack messages into lines of at most ~68 characters.  If a turn produced
  more than fits on one line, the player sees --More-- between them."
  [msgs]
  (reduce (fn [lines m]
            (let [cur (peek lines)]
              (if (and cur (<= (+ (count cur) 1 (count m)) 68))
                (conj (pop lines) (str cur " " m))
                (conj lines m))))
          [] msgs))

(defn show-messages! [state]
  (let [lines (paginate (:msgs state))]
    (doseq [l (butlast lines)]
      (draw! state (str l " --More--"))
      (read-key))
    (draw! state (or (get-in state [:mode :prompt]) (last lines) ""))))

(defn game-loop
  "Read a key, update the state, draw, repeat -- until the game ends.
  Returns the final state."
  [state]
  (loop [state state]
    (cond
      (or (:dead state) (:won state))
      (do (show-messages! state)
          ;; Pause so the player can read the final messages -- except when
          ;; they chose to quit, where waiting would just be annoying.
          (when-not (= "quit" (:dead state)) (read-key))
          state)

      (:overlay state)
      (do (draw! state "") (read-key)
          (recur (-> state (dissoc :overlay) (assoc :msgs []))))

      :else
      (do (show-messages! state)
          (recur (handle-key state (read-key)))))))

(defn ending-text [state]
  (let [gold (get-in state [:player :gold])]
    (cond
      (:won state)
      (str "\nYou escaped the Dungeons of Doom with the Amulet of Yendor!\n"
           "You finished with " (+ gold 5000) " points.\n")

      (= "quit" (:dead state))
      (str "\nYou left the dungeon on level " (:depth state)
           " with " gold " gold.  Goodbye!\n")

      :else
      (str "\n        ____________\n"
           "       /            \\\n"
           "      /     R.I.P.   \\\n"
           "     |   " (format "%-12s" "adventurer") " |\n"
           "     |  " (format "%-14s" (str "level " (:depth state))) "|\n"
           "     |  " (format "%-14s" (str gold " gold")) "|\n"
           "   __|________________|__\n\n"
           "You died on level " (:depth state) ", " (:dead state)
           ", with " gold " gold.\n"))))

(defn -main [& _]
  (let [saved (try (sh! "stty -g < /dev/tty") (catch Exception _ ""))
        size  (try (sh! "stty size < /dev/tty") (catch Exception _ ""))]
    (when (or (str/blank? saved) (str/blank? size))
      (println "Rogue needs an interactive terminal.")
      (System/exit 1))
    (let [[rows cols] (map #(Integer/parseInt %) (str/split size #" "))]
      (when (or (< rows 24) (< cols 80))
        (println (str "Please enlarge your terminal to at least 80x24 (it is "
                      cols "x" rows ")."))
        (System/exit 1)))
    (let [restored? (atom false)
          restore!  (fn []
                      ;; Idempotent: it runs normally AND from the shutdown
                      ;; hook, but must only act once (a second "leave
                      ;; alternate screen" could move the cursor).
                      (when (compare-and-set! restored? false true)
                        (print (str ESC "[?25h" ESC "[?1049l"))  ; show cursor, leave alt screen
                        (flush)
                        (sh! (str "stty " saved " < /dev/tty"))))]
      ;; If the user presses Ctrl-C the JVM exits; this hook still puts the
      ;; terminal back the way we found it.
      (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable restore!))
      (let [final (try
                    (sh! "stty -icanon -echo min 1 time 0 < /dev/tty")
                    (print (str ESC "[?1049h" ESC "[?25l" ESC "[2J"))
                    (game-loop (new-game))
                    (finally (restore!)))]
        (println (ending-text final))))))

;; Run the game when this file is executed as a script.  (The test harness
;; sets rogue.no-main so it can load the code without starting a game.)
(when-not (System/getProperty "rogue.no-main")
  (-main)
  (System/exit 0))


;; =============================================================================
;;  EXERCISES FOR STUDENTS
;; =============================================================================
;;  1. Add a new monster (one line in `monster-types`).  Is that enough for
;;     it to start appearing?  Why?  (Hint: look at `monster-order`.)
;;  2. Add a new potion type, e.g. :haste.  Which tables and functions must
;;     change?  (potion-types, random-item, quaff ... follow the data!)
;;  3. Monsters chase greedily and get stuck behind walls.  Replace `chase`
;;     with a breadth-first search for a real shortest path.
;;  4. Add traps (hidden tiles revealed by the 's' search command).
;;  5. Add rings and wands/staffs, which the original had and we left out.
;;  6. Make levels persistent so that going up returns to the SAME level.
;;     (Hint: keep a map of depth -> level inside the state.)
;;  7. Write tests with clojure.test.  `handle-key` is pure, so a test can
;;     build a state, send keys, and check the result with no terminal.