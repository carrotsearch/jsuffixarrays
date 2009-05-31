#!/usr/bin/ruby

#
# Script for generating corpus summary table. Body of latex table, to be exact.
#


$algorithms = ["BPR", "DIVSUFSORT", "QSUFSORT", "SKEW"]
$map = {}

def header()
  print"\\begin{tabular}{|l|"
  $algorithms.each { |alg|
    print "r|"
  }
  puts "} \\hline"
  $algorithms.each { |alg|
   print " & \\emph{" + alg.downcase + "}" 
  }
  puts "\\\\ \\hline"
  
end

def line(path, nameIndex)
  min = Float::MAX
  minId = ''
 	File.open(path).each { |line|

     tokens = line.split
     $map[tokens[nameIndex]] = tokens[0]
     if tokens[0].to_f < min then
       min = tokens[0].to_f
       minId = tokens[nameIndex]      
     end


 	}
 	$map[minId] = "\\textbf{" + $map[minId].to_s + "}"
 	$algorithms.each { |alg|
       print " & " + $map[alg]
       $map[alg] = ""
   }
     puts "\\\\"
end


#start
flag = 1
Dir.foreach(ARGV[0]) { |f|
  next if f.index('.') == 0
  header() if flag == 1
  flag = 0
  print "\\texttt{" + f.gsub("_", "\\_") + "}"
  $algorithms.each { |alg|
    $map[alg] = "oom"
  }
  line(ARGV[0] + "/" + f + "/averages",2)
}

#
# summary 
# 
puts " \\hline"

print "Total"
line(ARGV[0] + ".sum.log", 1)
puts " \\hline"
puts "\\end{tabular}"



