

def countdown(n):
    if n <= 0: #base case
        print("Liftoff!")
        return
    print(n)
    countdown(n -1) #recursive case
        #waits until countdown(n-1) finishes


countdown(5)
#print(5)
#coutdown(4)
    #print(4)
    #coutdown(3)
        #print(3)
        #countdown(2)
            #print(2)
            #coutdown(1)
                #print(1)
                #countdown(0)
                    #print("Liftoff!")
                    #exit

def sum_list(items):
    if not items:
        return 0 #base case
    return items[0] + sum_list(items[1:]) #recursive case
    #items[1:] ->   [StartingIndex:EndingIndex]


print(sum_list([1, 2, 3, 4])) #10

sum_list([1, 2, 3, 4])
    #1 + 2 + 3 + 4 + 0

print(sum_list([]))
