class Node:
    def __init__(self, value, next_node=None):
        self.value = value
        self.next = next_node

class SnakeBody:
    def __init__(self):
        self.head = None

    def grow(self, segment_name):
        new_head = Node(segment_name, self.head)
        self.head = new_head

    def move(self):
        self.grow("new-head")
        if self.head is None or self.head.next is None:
            return
        current = self.head
        while current.next.next is not None:
            current = current.next
        current.next = None

    def print_body(self):
        segments = []
        current = self.head
        while current is not None:
            segments.append(current.value)
            current = current.next
        print(" -> ".join(segments))

snek = SnakeBody()
snek.grow("segment1")
snek.grow("segment2")
snek.grow("segment3")
snek.print_body()

snek.move()
snek.print_body()
