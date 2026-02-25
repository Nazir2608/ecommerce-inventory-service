local stock = tonumber(redis.call("GET", KEYS[1]))
local qty = tonumber(ARGV[1])

if not stock then
    return -2 -- Key missing
end

if stock >= qty then
    return redis.call("DECRBY", KEYS[1], qty)
else
    return -1 -- Insufficient stock
end