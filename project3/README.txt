Project #3 Completed by Zixia Weng on Feb.23rd.2019

For SpellChecker2.java, I made follwing improvements:

In method public double calculateEM(String word, String correction, double editDistance), I added 2 conditions to return different scores/probabilities. 
1. When the length of word and correction are the same, we compare each characters one by one, if all of the different characters are vowels, which means it's possibly a typo, should be corrected, so we return 1.0. Else we return 0.2. This boost our accuracy from 74% to 75%.

2. When the length of word and correction are different, we create 2 hashset for each word. Then I check the difference between 2 hashset, if all of the different character in set are vowels, return 1.0. Else we return 0.2. This boost our accuracy from 75% to 76.6%.