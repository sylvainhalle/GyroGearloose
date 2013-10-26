<?php

// Get filenames used as samples
for ($i = 0; $i < count($args); $i++)
{
  $files[] = $args[$i];
}

for ($threshold = 40; $threshold <= 75; $threshold += 5)
{
  foreach ($files as $filename)
  {
    exec("convert $filename -threshold $threshold% $out_temp_filename");
    // TODO
  }
}

?>
