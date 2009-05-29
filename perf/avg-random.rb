#!/usr/bin/ruby

#
# Calculate truncated average from a random input test  log file. 
#

map = {}


$stdin.each_line do |line|
    row_values = line.split
    next if line.empty? or not row_values[0] =~ /[0-9]+/ or row_values[0].to_i < 0
    
    map[row_values[0]] = [] unless map.has_key? row_values[0]
    row_values.each_index do |i|
        map[row_values[0]].push([]) if         map[row_values[0]].length <= i
        map[row_values[0]][i] << row_values[i]
    end
end

def avg(a)
    a = (a.map {|e| e.to_f})
    sum = a.inject(0) {|sum,v| sum + v}
    sum / a.size
end

def stddev(a)
  begin
    a = (a.map {|e| e.to_f})
    squares_sum = a.inject(0) {|sum,v| sum + v ** 2}
    stddev = Math.sqrt((squares_sum - a.size * (avg(a) ** 2)) / (a.size - 1))
  rescue 
    return -1
  end
end



map.keys.sort.each do |key|
  puts "%s %d %.4f %.4f %.4f %.4f" % [key, map[key][1][0],   avg(map[key][2]), stddev(map[key][2]), avg(map[key][3]), stddev(map[key][3])]

end


