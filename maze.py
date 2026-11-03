import random

def generate_maze(width, height):
    """Generate a maze using recursive backtracking.
    Returns a grid where True = wall, False = open path.
    Grid dimensions are (2*width+1) x (2*height+1) to leave room
    for walls between cells.
    """
    grid_w, grid_h = width * 2 + 1, height * 2 + 1
    grid = [[True for _ in range(grid_w)] for _ in range(grid_h)]
    visited = [[False for _ in range(width)] for _ in range(height)]

    def carve(cx, cy):
        visited[cy][cx] = True
        grid[cy * 2 + 1][cx * 2 + 1] = False  # open up this cell

        directions = [(0, -1), (0, 1), (-1, 0), (1, 0)]
        random.shuffle(directions)

        for dx, dy in directions:
            nx, ny = cx + dx, cy + dy
            if 0 <= nx < width and 0 <= ny < height and not visited[ny][nx]:
                # knock down the wall between (cx, cy) and (nx, ny)
                wall_x = cx * 2 + 1 + dx
                wall_y = cy * 2 + 1 + dy
                grid[wall_y][wall_x] = False
                carve(nx, ny)  # recursive call: explore from the new cell

    carve(0, 0)
    return grid


def print_maze(grid):
    for row in grid:
        print("".join("#" if cell else "." for cell in row))


maze = generate_maze(8, 5)
print_maze(maze)