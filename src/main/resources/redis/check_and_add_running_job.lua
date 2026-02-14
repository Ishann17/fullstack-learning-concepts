--KEYS[1] = running jobs SET key
--ARGV[1] = max allowed tier limit
--ARGV[2] = jobId to insert

-- Get current running job count from the SET
local current = redis.call('SCARD', KEYS[1])

-- Read the allowed limit passed from Java
local limit = tonumber(ARGV[1])

-- If limit reached, reject
if current >= limit then
	return 0
end

-- Otherwise add jobId to SET
redis.call('SADD', KEYS[1], ARGV[2])

-- Allow the request
return 1
