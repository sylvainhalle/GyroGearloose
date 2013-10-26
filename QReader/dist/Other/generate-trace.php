<?php

$num_events = 400;
$event_size = 600;

echo "<trace>\n";
for ($i = 0; $i < $num_events; $i++)
{
  echo "<event>#$i?".random_data($event_size)."</event>\n";
}
echo "</trace>\n";

function random_data($length)
{
  $out = "";
  for ($i = 0; $i < $length; $i++)
  {
    $out .= chr(rand(48, 126));
  }
  return $out;
}
?>
