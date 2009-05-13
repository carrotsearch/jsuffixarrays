#!/usr/bin/ruby

#
# Calculate truncated average from a corpus-stats log file. 
#

columns = []

$stdin.each_line do |line|
    row_values = line.split
    next if line.empty? or not row_values[0] =~ /[0-9]+/ or row_values[0].to_i < 0

    row_values.each_index do |i|
        columns.push([]) if columns.length <= i
        columns[i] << row_values[i]
    end
end

def avg(a)
    a = (a.map {|e| e.to_f})
    sum = a.inject(0) {|sum,v| sum + v}
    sum / a.size
end

def stddev(a)
    a = (a.map {|e| e.to_f})

    squares_sum = a.inject(0) {|sum,v| sum + v ** 2}
    stddev = Math.sqrt((squares_sum - a.size * (avg(a) ** 2)) / (a.size - 1))
end

puts "%.4f %.4f %s" % [avg(columns[2]), stddev(columns[2]), columns[6][0].gsub("_", "-")]

