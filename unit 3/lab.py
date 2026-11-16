enemies = [
    {"name": "goblin", "hp": 10, "attack": 3},
    {"name": "slime", "hp": 5, "attack": 1},
    {"name": "dragon", "hp": 100, "attack": 25},
    {"name": "bat", "hp": 7, "attack": 2},
]

def is_weak(enemy):
    return enemy["hp"] < 20

def battle_report(enemies):
    # TODO 1: build a list of every enemy's name
    names = None

    # TODO 2: build a list of names of only the weak enemies (use is_weak)
    weak = None

    # TODO 3: sum up every enemy's hp
    total_hp = None

    # TODO 4: find the enemy dict with the highest "attack" value
    strongest = None

    return {
        "roster": names,
        "weak_enemies": weak,
        "total_hp": total_hp,
        "strongest": strongest["name"] if strongest else None,
    }

print(battle_report(enemies))
