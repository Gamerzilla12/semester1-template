class Stack:
    def __init__(self):
        self._items = []

    def push(self, item):
        self._items.append(item)

    def pop(self):
        if not self._items:
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self):
        return self._items[-1]

    def is_empty(self):
        return len(self._items) == 0

    def __len__(self):
        return len(self._items)


class UndoRedoSystem:
    def __init__(self):
        self.history = Stack()
        self.redo_stack = Stack()


    def do_action(self, action):
        self.history.push(action)
        #TODO: a new action should clear the redo stack


    def undo(self):
        if self.history.is_empty():
            return None
        action = self.history.pop()
        self.redo_stack.push(action)
        return action


    def redo(self):
        #TODO: pop from redo_stack, push back onto history, return the action
        pass    
