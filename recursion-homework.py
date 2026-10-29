# write a function that recurses through a list printing the values

def recur_print(lst):
    # look at sum_list
    #print
    #recur
    pass


recur_print(["Larry", "Moe", "Curly"])


# add a stack trace to sum_list
def sum_list(items, depth=0):
    if not items:
        return 0 #base case
    return items[0] + sum_list(items[1:]) #recursive case
    #items[1:] ->   [StartingIndex:EndingIndex]