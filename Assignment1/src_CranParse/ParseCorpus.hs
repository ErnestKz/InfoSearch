import qualified System.IO                   as IO
import qualified System.Directory            as Dir
import Data.Text            (strip, pack, unpack)

strip' = unpack . strip . pack 

main :: IO ()
main = do
  parseAndGenFiles "../Cran/cran.all.1400" "../CranCorpus/"
  parseAndGenFiles "../Cran/cran.qry"      "../CranQueries/"
  
parseAndGenFiles :: String -> String -> IO ()
parseAndGenFiles srcFile dstDir = IO.withFile srcFile IO.ReadMode
       (\h -> IO.hGetContents h >>=
         \fileContents -> do
           let (lbs',lastLabel,lastContent) = foldl collectLabelContentPairs ([],'x',"") $ lines fileContents
               lbs        = tail lbs' ++ [(lastLabel, lastContent)]  -- get rid of dummy first element and add the last label,content pair
               (idCorpusPairs, _) = foldl collectIdDocPairs ([], Nothing) lbs -- collect associated Id with Corpus Body
               
           Dir.createDirectoryIfMissing True dstDir
           mapM_ (\(corpusId, corpusBody) -> writeFile 
                                             (dstDir ++ corpusId)
                                             corpusBody) idCorpusPairs
           return ())

type Label = Char                                -- one of ITABW
type SectionContent = String                     -- the content that follows a label
type LabelContentPair = (Label, SectionContent)  -- the Label and associated content of the section together.

type CorpusId = String                        -- the content of the 'I' label.
type Corpus = String                          -- the content of the 'W' label.
type IdCorpusPair = (CorpusId, Corpus)        -- the content of 'I' and the associated 'W' put together.

charLabels :: [ Label ]  
charLabels = "ITABW"    -- The labels we expect in the file.


-- The below function is applied to each line of the input file cran.all.1400.
-- It collects the each LabelContentPair into a list.

-- Each section in the file is delimited by a '.X' where X is the Label for the section.

-- It collects the SectionContent as it goes line by line through the file, collecting the contents and keeping track of
-- the Label thats associated with it, and when it encounters a new Label in line, then it puts
-- the SectionContent together with Label it was associated with into the accumlated list of 'LabelContentPair'

collectLabelContentPairs :: ([LabelContentPair], Label, SectionContent) -> String -> ([LabelContentPair], Label, SectionContent)
collectLabelContentPairs (labelContentPairs,  -- list of LabelContentPair so far in the file
                       currentLabel,          -- the last label seen
                       currentSectionContent) -- the content since the last label
  currentLine@(c1:c2:restOfLine)              -- the current line that is being processed,
                                              -- c1 and c2, being first and second char of the line

  -- this checks if the beginning of the line is of the form '.X' where X is an element of the charLabels i.e ITABW
  -- if so, then add our currently tracked label and section into the list of LabelContentPair
  | c1 == '.' && (elem c2 charLabels) = (labelContentPairs++[(currentLabel, currentSectionContent)]  -- add LabelContentPair to list
                                        ,c2                                                          -- the second character of the line becomes the last seen label
                                        ,strip' restOfLine)                                          -- the rest of the line line will be set as the content since the label

  -- otherwise it is an ordinary line so append the contents of the line to the conent that we've seen since the last label
  | otherwise                         = (labelContentPairs                                 
                                        ,currentLabel
                                        ,currentSectionContent ++ (' ':currentLine))
                                        

-- This function goes through all the LabelContentPairs
-- that we have and puts the 'I' and 'W' contents together, corresponding
-- to the corpus id and corpus body.

collectIdDocPairs :: ([IdCorpusPair], Maybe CorpusId) -> LabelContentPair -> ([IdCorpusPair], Maybe CorpusId)
collectIdDocPairs (idCorpusPairs, Nothing)  ('I', content) = (idCorpusPairs, Just content)                     -- we find a corpusId, so place it as the last corpusId seen
collectIdDocPairs (idCorpusPairs, Just cId) ('W', content) = (idCorpusPairs++[(cId, content)], Nothing)        -- we find a corpus body, so put our corpusId together with the corpusBody
collectIdDocPairs acc _ = acc -- not interested in other labels


