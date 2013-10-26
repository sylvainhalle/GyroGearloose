<?php

$num_messages = 200;
$min_pingus = 25;
$max_pingus = 25;

for ($i = 0; $i < $num_messages; $i++)
{
  echo "0 {\n";
  echo "  \"deadlyvelocity\" : ".rand(0, 10).",\n";
  echo "  \"pingus\" : [\n";
  $pingus_this_time = rand($min_pingus, $max_pingus);
  for ($j = 0; $j < $pingus_this_time; $j++)
  {
    if ($j > 0)
      echo ",\n";
    echo "    {\n";
    echo "      \"id\" : ".$j.",\n";
    echo "      \"x\" : ".$i.",\n";
    echo "      \"y\" : ".rand(0, 10).",\n";
    echo "      \"status\" : \"walker\"\n";
    echo "    }";
  }
  echo "\n";
  echo "  ]\n";
  echo "}\n---\n";
}

?>
