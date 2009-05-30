#!/usr/bin/ruby

# algorithm => time
timeMap = {}

# algorithm => occurences
countMap = {}

#
# Sum up times of each algorithm for every file from corpus
# 
Dir.foreach(ARGV[0]) { |f|
	next if f.index('.') == 0
	File.open(ARGV[0] + "/" + f + "/averages").each { |line|
		tokens = line.split
		timeMap[tokens[2]] = 0 unless timeMap.has_key? tokens[2]
		countMap[tokens[2]] = 0 unless countMap.has_key? tokens[2]
		timeMap[tokens[2]] += tokens[0].to_f
		countMap[tokens[2]] += 1

	}
}

#
# Check whether every algorithm has results for every file
# If not - skip it when printing summary
#
maxCount = -1
timeMap.keys.each do |key|
  maxCount = countMap[key] if countMap[key] > maxCount
end

timeMap.keys.each do |key|
	puts "%.4f %s" % [timeMap[key],key] unless maxCount > countMap[key]
end
