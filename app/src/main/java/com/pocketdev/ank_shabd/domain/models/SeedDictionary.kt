package com.pocketdev.ank_shabd.domain.models

import com.pocketdev.ank_shabd.domain.encoder.EncodingRuleSet
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder

object SeedDictionary {

    data class RawSeedEntry(
        val word: String,
        val expectedCode: String,
        val category: String,
        val meaningHi: String,
        val meaningEn: String,
        val transliteration: String,
        val qualityGrade: String, // "A", "B", "C"
        val visualAssociation: String? = null,
        val exampleSentenceHi: String? = null,
        val exampleSentenceEn: String? = null,
        val mnemonicStory: String? = null,
        val whyGoodPeg: String? = null
    )

    // Known unassigned slots from V2.3 audit (Section 2, 11)
    val UNRESOLVED_SLOTS_00_99 = setOf("36", "46", "55", "67", "95")

    val SEED_ENTRIES = listOf(
        // Single-digit pegs (0-9) for chunking & direct lookup
        RawSeedEntry("शह", "0", "संज्ञा", "शतरंज में राजा को दी जाने वाली चुनौती (शह)", "check in chess", "shah", "A", "शतरंज के खेल में राजा को शह", mnemonicStory = "एक विशाल शतरंज के बोर्ड पर सोने के राजा को अचानक 'शह' मिलती है और घंटी बज उठती है।"),
        RawSeedEntry("कह", "1", "क्रिया", "बोलना, कहना, बात बताना", "say, speak, tell", "kah", "A", "माइक पर ज़ोर से बात कहना", mnemonicStory = "एक बड़े सुनहरे माइक पर खड़ा वक्ता पूरे मैदान से कुछ महत्वपूर्ण 'कह' रहा है।"),
        RawSeedEntry("दल", "2", "संज्ञा", "समूह, टोली, फूल की पंखुड़ी", "group, team, petal", "dal", "A", "सैनिकों या पर्वतारोहियों का दल", mnemonicStory = "रंग-बिरंगे झंडे लिए पर्वतारोहियों का एक उत्साही 'दल' बर्फीली चोटी पर चढ़ रहा है।"),
        RawSeedEntry("तल", "3", "संज्ञा", "सतह, नीचे का भाग, पेंदी", "bottom, surface, floor", "tal", "A", "समुद्र का गहरा चमकीला तल", mnemonicStory = "पारदर्शी नीले समुद्र के सबसे निचले 'तल' पर मोतियों की चादर बिछी हुई है।"),
        RawSeedEntry("जल", "4", "संज्ञा", "पानी, नीर, जीवनदायी तरल", "water, aqua", "jal", "A", "चाँदी के कलश से गिरता स्वच्छ जल", mnemonicStory = "चाँदी के बड़े कलश से ठंडा और चमकता हुआ 'जल' झरने की तरह बह रहा है।"),
        RawSeedEntry("फल", "5", "संज्ञा", "पेड़ का मीठा खाद्य फल, परिणाम", "fruit, result", "phal", "A", "टोकरी में रखे ताजे रसीले फल", mnemonicStory = "सोने की टोकरी में चमकते हुए विशाल लाल और पीले 'फल' रखे हैं जिनसे मीठी सुगंध आ रही है।"),
        RawSeedEntry("छह", "6", "संज्ञा/संख्या", "संख्या छह (6)", "number six", "chhah", "A", "क्रिकेट में छक्का (छह रन)", mnemonicStory = "स्टेडियम में बल्लेबाज ने ऐसी गेंद मारी कि वह सीधे आसमान पार करके 'छह' रन बन गई।"),
        RawSeedEntry("हँस", "7", "क्रिया", "हँसना, मुस्कुराना, प्रसन्न होना", "laugh, smile", "hans", "A", "खुलकर ठहाके लगाकर हँसना", mnemonicStory = "एक गुब्बारे जैसा गोल चेहरा इतनी ज़ोर से 'हँस' रहा है कि सारा कमरा गूँज उठा।"),
        RawSeedEntry("हवा", "8", "संज्ञा", "पवन, वायु, समीर", "wind, air, breeze", "hawaa", "A", "पेड़ों को झुलाती तेज़ ठंडी हवा", mnemonicStory = "पहाड़ों से आती तेज़ और सुगंधित 'हवा' में रंग-बिरंगे दुपट्टे आसमान में लहरा रहे हैं।"),
        RawSeedEntry("नल", "9", "संज्ञा", "पानी का नल, टोटी", "water tap, faucet", "nal", "A", "ताँबे के नल से निकलती पानी की धार", mnemonicStory = "बगीचे के बीच एक विशाल सुनहरा 'नल' लगा है जिससे फव्वारे की तरह पानी निकल रहा है।"),

        // 00-24
        RawSeedEntry("शूल", "00", "संज्ञा", "नुकीला अस्त्र, काँटा", "spike, sharp weapon, thorn", "shool", "B", "नुकीला काँटा चुभना"),
        RawSeedEntry("शक", "01", "संज्ञा", "संदेह, संशय", "doubt, suspicion", "shak", "A", "संदेह की निगाह"),
        RawSeedEntry("उदय", "02", "संज्ञा", "उगना, प्रकट होना", "sunrise, emergence, dawn", "uday", "A", "सूर्य का उदय"),
        RawSeedEntry("शहद", "02", "संज्ञा", "मधुमक्खी का मीठा रस", "honey", "shahad", "A", "शुद्ध मीठा शहद", mnemonicStory = "काँच के जार से सुनहरा और गाढ़ा 'शहद' चम्मच पर चमकते हुए गिर रहा है।"),
        RawSeedEntry("शील", "03", "संज्ञा", "आचरण, चरित्र, स्वभाव", "good conduct, character, modesty", "sheel", "A", "सज्जन का शील स्वभाव"),
        RawSeedEntry("शहर", "04", "संज्ञा", "नगर, बड़ा आबादी वाला क्षेत्र", "city, town", "shahar", "A", "ऊंची इमारतों वाला शहर", mnemonicStory = "रात की रोशनी में जगमगाता हुआ एक भव्य 'शहर' देखो, जिसकी ऊँची इमारतें बादलों को छू रही हैं।"),
        RawSeedEntry("उपल", "05", "संज्ञा", "पत्थर, ओला, रत्न", "stone, hailstone, gem", "upal", "B", "चमकता हुआ पत्थर"),
        RawSeedEntry("उछल", "06", "क्रिया", "उछलना, छलांग लगाना", "leap, bound, jump", "uchhal", "A", "हर्ष से उछल पड़ना"),
        RawSeedEntry("उस", "07", "सर्वनाम", "वह का तिर्यक रूप", "that, him, her", "us", "A", "उस व्यक्ति की बात"),
        RawSeedEntry("शाल", "08", "संज्ञा", "ओढ़ने का गर्म वस्त्र, शाल-वृक्ष", "shawl, sal tree", "shaal", "B", "सर्दियों में गर्म शाल"),
        RawSeedEntry("ऊन", "09", "संज्ञा", "भेड़ के बालों से बना रेशा", "wool", "oon", "A", "ऊन का गर्म स्वेटर"),
        RawSeedEntry("कुल", "10", "संज्ञा", "वंश, परिवार, समग्र", "family, clan, total", "kul", "A", "कुल जमा राशि"),
        RawSeedEntry("खेल", "11", "संज्ञा", "क्रीड़ा, मनोरंजन, खेल-कूद", "game, play, sport", "khel", "A", "मैदान में फुटबॉल का खेल", mnemonicStory = "एक बहुत बड़ा खेल का मैदान देखो, जहाँ गेंद आसमान जितनी बड़ी है और सभी लोग उत्साह से उसे खेल रहे हैं।"),
        RawSeedEntry("एक", "11", "संख्या/विशेषण", "संख्या एक, प्रथम", "number one, unity", "ek", "A", "विजयी ट्रॉफी नंबर एक", mnemonicStory = "मंच के बीचों-बीच सोने से बना विशाल अंक 'एक' (1) ट्रॉफी की तरह चमक रहा है।"),
        RawSeedEntry("केवल", "11", "अव्यय", "सिर्फ, मात्र, इकलौता", "only, solely", "keval", "A", "केवल एक चमकता तारा"),
        RawSeedEntry("गोल", "12", "विशेषण/संज्ञा", "वृत्ताकार, गेंद जैसा", "round, circular, sphere", "gol", "A", "गोल फुटबॉल"),
        RawSeedEntry("कोयल", "12", "संज्ञा", "मीठी आवाज वाली काली चिड़िया", "cuckoo bird", "koyal", "A", "आम की डाली पर गाती कोयल", mnemonicStory = "आम के हरे पेड़ पर बैठी एक सुरीली 'कोयल' पंचम सुर में गा रही है।"),
        RawSeedEntry("कील", "13", "संज्ञा", "लोहे या धातु की खूंटी", "nail, peg, pin", "keel", "A", "दीवार में लोहे की कील"),
        RawSeedEntry("कर", "14", "संज्ञा/क्रिया", "टैक्स, हाथ, क्रिया-रूप", "tax, hand, action", "kar", "A", "सरकारी कर का भुगतान"),
        RawSeedEntry("घर", "14", "संज्ञा", "मकान, आवास, निवास स्थान", "home, house", "ghar", "A", "सुंदर बगीचे वाला अपना घर", mnemonicStory = "पहाड़ियों के बीच लाल छत और सुंदर बगीचे वाला एक प्यारा सा 'घर' चमक रहा है।"),
        RawSeedEntry("कप", "15", "संज्ञा", "चाय-कॉफी का पात्र", "cup", "kap", "A", "गर्म चाय का प्याला"),
        RawSeedEntry("कछ", "16", "संज्ञा", "कछुआ से संबंधित स्थानिक शब्द", "turtle, aquatic context", "kachh", "B", "जलीय क्षेत्र"),
        RawSeedEntry("कस", "17", "क्रिया", "कसना, मजबूती से बांधना", "tighten, grip, bind", "kas", "A", "रस्सी कस कर बांधना"),
        RawSeedEntry("गाय", "18", "संज्ञा", "पालतू दुधारू पशु", "cow", "gaay", "A", "सफेद रंग की शांत गाय", mnemonicStory = "हरी घास के मैदान में एक चमकती सफेद 'गाय' खड़ी है जिसके गले में सोने की घंटी बज रही है।"),
        RawSeedEntry("गाल", "18", "संज्ञा", "मुख का पार्श्व भाग, कपोल", "cheek", "gaal", "A", "बच्चे के लाल गाल", mnemonicStory = "एक मुस्कुराते हुए बच्चे के सेब जैसे गोल और लाल 'गाल' धूप में चमक रहे हैं।"),
        RawSeedEntry("काल", "18", "संज्ञा", "समय, युग, मृत्यु", "time, era, period", "kaal", "A", "प्राचीन काल का इतिहास"),
        RawSeedEntry("गाँव", "18", "संज्ञा", "ग्रामीण बस्ती (चंद्रबिंदु वर्तनी)", "village", "gaanv", "A", "हरा-भरा सुंदर गाँव"),
        RawSeedEntry("कमल", "19", "संज्ञा", "कमल का फूल, जलज", "lotus flower", "kamal", "A", "तालाब में खिला गुलाबी कमल", mnemonicStory = "शांत नीले सरोवर के बीचों-बीच एक विशाल गुलाबी 'कमल' खिला है जिस पर ओस की बूँदें हीरे की तरह चमक रही हैं।"),
        RawSeedEntry("क़लम", "19", "संज्ञा", "लिखने की लेखनी, पेन", "pen, reed pen", "qalam", "A", "सुंदर लिखावट वाली क़लम"),
        RawSeedEntry("धूल", "20", "संज्ञा", "मिट्टी के बारीक कण, गर्द", "dust, dirt", "dhool", "A", "रास्ते पर उड़ती धूल", mnemonicStory = "सूरज की किरणों में रेगिस्तान की सुनहरी 'धूल' हवा में चमकते बादल की तरह उड़ रही है।"),
        RawSeedEntry("देह", "21", "संज्ञा", "शरीर, काया", "body, physique", "deh", "A", "स्वस्थ मानव देह"),
        RawSeedEntry("ढोल", "22", "संज्ञा", "चमड़े से मढ़ा वाद्य यंत्र", "drum, tom-tom", "dhol", "A", "त्योहार पर बजता बड़ा ढोल", mnemonicStory = "उत्सव के मैदान में एक विशाल रंग-बिरंगा 'ढोल' ज़ोर-ज़ोर से बज रहा है।"),
        RawSeedEntry("दही", "23", "संज्ञा", "दूध को जमा कर बना खाद्य पदार्थ", "curd, yogurt", "dahi", "A", "मिट्टी की मटकी में ताजा दही", mnemonicStory = "मिट्टी की ठंडी मटकी में बर्फ जैसा सफेद और गाढ़ा 'दही' भरा है जिसे चाँदी के चम्मच से परोसा जा रहा है।"),
        RawSeedEntry("दर", "24", "संज्ञा", "भाव, मूल्य, चौखट/द्वार", "rate, price, doorway", "dar", "A", "बाजार की उचित दर", mnemonicStory = "बाज़ार के भव्य मुख्य द्वार पर सोने के अक्षरों में आज की मूल्य 'दर' चमक रही है।"),
        RawSeedEntry("डर", "24", "संज्ञा", "भय, खौफ, आतंक", "fear, fright", "dar", "A", "अंधेरे में अचानक लगा डर", mnemonicStory = "एक डरावनी परछाई को देखकर कार्टून पात्र के सिर के बाल 'डर' से सीधे खड़े हो गए हैं।"),
        RawSeedEntry("ओर", "24", "संज्ञा", "दिशा, तरफ़, छोर", "direction, side, towards", "or", "A", "सूरज उगने की ओर इशारा"),

        // 25-49
        RawSeedEntry("डफ", "25", "संज्ञा", "एक प्रकार का खंजरी वाद्य", "tambourine, small drum", "daf", "B", "हाथ से बजती डफली"),
        RawSeedEntry("ओझल", "26", "विशेषण", "दृष्टि से छिपा हुआ, गायब", "hidden, out of sight", "ojhal", "A", "धुंध में ओझल होना", mnemonicStory = "घने सफेद कोहरे और बादलों के बीच एक रहस्यमयी महल अचानक आँखों से 'ओझल' हो जाता है।"),
        RawSeedEntry("ओस", "27", "संज्ञा", "घास पर जमी जल-बूंदें", "dew drops", "os", "A", "सुबह घास पर चमकती ओस"),
        RawSeedEntry("दवा", "28", "संज्ञा", "औषधि, रोग निवारक वस्तु", "medicine, remedy", "dawa", "A", "बीमारी दूर करने वाली दवा", mnemonicStory = "एक चमकती हुई नीली शीशी में जादुई 'दवा' भरी है जिसकी एक बूँद से तुरंत नई ऊर्जा आ जाती है।"),
        RawSeedEntry("दाल", "28", "संज्ञा", "खाद्य दलहन", "lentils, pulse", "daal", "A", "गरमा-गरम पीली दाल"),
        RawSeedEntry("ओला", "28", "संज्ञा", "आसमान से गिरने वाला बर्फ का गोला", "hailstone", "ola", "A", "बारिश में गिरते सफेद ओले"),
        RawSeedEntry("दम", "29", "संज्ञा", "साँस, ताकत, जीवन-शक्ति", "breath, stamina, life", "dam", "A", "लंबी दौड़ में दम रखना"),
        RawSeedEntry("तू", "30", "सर्वनाम", "मध्यम पुरुष सर्वनाम", "you (singular/informal)", "too", "A", "तू मेरा मित्र है"),
        RawSeedEntry("तेल", "31", "संज्ञा", "चिकना तरल पदार्थ", "oil", "tel", "A", "सरसों का पीला तेल"),
        RawSeedEntry("तोल", "32", "संज्ञा/क्रिया", "वजन करना, माप", "weight, measuring", "tol", "A", "तराजू पर सही तोल"),
        RawSeedEntry("तिल", "33", "संज्ञा", "तिल का बीज, चेहरे का मस्सा", "sesame seed, mole", "til", "A", "सफेद तिल के लड्डू"),
        RawSeedEntry("तर", "34", "विशेषण", "गीला, भीगा हुआ", "wet, moist, soaked", "tar", "B", "पानी से तर-ब-तर"),
        RawSeedEntry("तप", "35", "संज्ञा", "तपस्या, साधना, कठोर श्रम", "penance, austerity, meditation", "tap", "A", "ऋषि का कठोर तप"),
        // 36 is empty in V2.3
        RawSeedEntry("इस", "37", "सर्वनाम", "यह का तिर्यक रूप", "this, this one", "is", "A", "इस पुस्तक को पढ़ें"),
        RawSeedEntry("तवा", "38", "संज्ञा", "रोटी पकाने का धातु का बर्तन", "griddle, flat pan", "tawa", "A", "गर्म लोहे का तवा"),
        RawSeedEntry("तन", "39", "संज्ञा", "शरीर, काया", "body, physique", "tan", "A", "स्वस्थ तन और शांत मन"),
        RawSeedEntry("रूह", "40", "संज्ञा", "आत्मा, प्राण", "soul, spirit", "rooh", "B", "पवित्र रूह"),
        RawSeedEntry("रेल", "41", "संज्ञा", "रेलगाड़ी", "train, railway", "rel", "A", "पटरी पर दौड़ती रेल"),
        RawSeedEntry("जौ", "42", "संज्ञा", "एक प्रमुख प्राचीन अनाज (यव)", "barley grain", "jau", "A", "सुनहरे जौ की बालियाँ", mnemonicStory = "खेत में सोने की तरह चमकती हुई 'जौ' की लंबी बालियाँ हवा में झूम रही हैं।"),
        RawSeedEntry("रोल", "42", "संज्ञा", "भूमिका, लपेटा हुआ कागज़", "role, scroll, roll", "rol", "B", "नाटक में प्रमुख रोल", mnemonicStory = "मंच के केंद्र में कलाकार एक विशाल सुनहरा कागज़ का 'रोल' खोलकर अपना प्रमुख अभिनय कर रहा है।"),
        RawSeedEntry("चील", "43", "संज्ञा", "शिकारी बड़ा पक्षी", "kite bird, hawk", "cheel", "A", "आसमान में उड़ती चील"),
        RawSeedEntry("चर", "44", "क्रिया/संज्ञा", "चरना, बदलने वाला मान", "graze, variable", "char", "B", "मैदान में गाय का चरना"),
        RawSeedEntry("जलज", "44", "संज्ञा", "कमल का फूल (जल में जन्मा)", "lotus (born in water)", "jalaj", "A", "सरोवर में खिला सुंदर जलज", mnemonicStory = "नीले जल के भीतर से एक चमकता हुआ सफेद-गुलाबी 'जलज' (कमल) ऊपर उठ रहा है।"),
        RawSeedEntry("जप", "45", "संज्ञा", "मंत्र का निरंतर उच्चारण", "chanting, meditation", "jap", "A", "माला फेर कर जप करना"),
        // 46 is empty in V2.3
        RawSeedEntry("रस", "47", "संज्ञा", "स्वाद, आनंद, फलों का अर्क", "juice, essence, aesthetic relish", "ras", "A", "मीठे आम का रस", mnemonicStory = "काँच के बड़े जग में ताज़े फलों का सुनहरा और मीठा 'रस' भरा है जिससे चारों ओर सुगंध फैल रही है।"),
        RawSeedEntry("चाय", "48", "संज्ञा", "एक लोकप्रिय गर्म पेय", "tea", "chaay", "A", "अदरक वाली गरमा-गरम चाय"),
        RawSeedEntry("रन", "49", "संज्ञा", "क्रिकेट में बनाया जाने वाला अंक", "run in cricket", "ran", "B", "बल्लेबाज का तेज रन"),

        // 50-74
        RawSeedEntry("फूल", "50", "संज्ञा", "पुष्प, सुमन", "flower, blossom", "phool", "A", "सुगंधित लाल गुलाब का फूल"),
        RawSeedEntry("फेल", "51", "विशेषण", "असफल होना, अनुत्तीर्ण", "fail, unsuccessful", "phel", "B", "परीक्षा में फेल होना"),
        RawSeedEntry("पोल", "52", "संज्ञा", "खंभा या गुप्त भेद", "pole, secret, hollow", "pol", "B", "बिजली का लंबा पोल"),
        RawSeedEntry("अंत", "53", "संज्ञा", "समाप्ति, छोर, परिणाम", "end, finish, conclusion", "ant", "A", "कहानी का सुखद अंत"),
        RawSeedEntry("पहर", "54", "संज्ञा", "तीन घंटे का समय काल", "watch of day, period", "pahar", "A", "दोपहर की धूप"),
        // 55 is empty in V2.3
        RawSeedEntry("पछ", "56", "संज्ञा", "क्षेत्रीय/पुराना रूप", "regional archaic form", "pachh", "B", "पुराना संदर्भ"),
        RawSeedEntry("हंस", "57", "संज्ञा", "सफेद रंग का सुंदर जलीय पक्षी", "swan", "hans", "A", "सरोवर में तैरता सफेद हंस", mnemonicStory = "शांत नीली झील में एक मोती जैसा चमकता सफेद 'हंस' तैर रहा है जिसकी चोंच में चमकता मोती है।"),
        RawSeedEntry("पाल", "58", "संज्ञा/क्रिया", "नाव का परदा, पालना", "sail of boat, nurture", "paal", "A", "नाव का हवा में लहराता पाल"),
        RawSeedEntry("फन", "59", "संज्ञा", "साँप का फन, कला या हुनर", "snake hood, artistic skill", "phan", "A", "कोबरा का फैला हुआ फन"),
        RawSeedEntry("झूल", "60", "क्रिया/संज्ञा", "झूलना, झूला", "swing", "jhool", "B", "पेड़ पर झूला झूलना"),
        RawSeedEntry("झेल", "61", "क्रिया", "सहन करना, बर्दाश्त करना", "endure, suffer, bear", "jhel", "A", "मुसीबत को धीरज से झेलना"),
        RawSeedEntry("झोल", "62", "संज्ञा", "ढीलापन, थैली, उलझन", "pouch, snag, loose fold", "jhol", "A", "काम में कोई झोल होना"),
        RawSeedEntry("झील", "63", "संज्ञा", "प्राकृतिक जल-राशि", "lake", "jheel", "A", "पहाड़ों के बीच नीली झील"),
        RawSeedEntry("झर", "64", "क्रिया", "गिरना, झरना", "cascade, shed, trickle", "jhar", "B", "पत्तों का पेड़ से झरना"),
        RawSeedEntry("झप", "65", "संज्ञा", "झपकी, अचानक झपटना", "blink, sudden nap, pounce", "jhap", "B", "आँख की हल्की झपकी"),
        RawSeedEntry("छः", "66", "संज्ञा", "संख्या छः (संस्कृतनिष्ठ वर्तनी)", "six (orthographic variant)", "chhah", "B", "छः की संख्या"),
        // 67 is empty in V2.3
        RawSeedEntry("छाल", "68", "संज्ञा", "पेड़ का बाहरी आवरण", "bark of tree", "chhaal", "A", "नीम के पेड़ की छाल"),
        RawSeedEntry("छन", "69", "संज्ञा", "ध्वनि-अनुकरण, पायल की झंकार", "jingle sound, filter action", "chhan", "B", "पायल की छन-छन आवाज"),
        RawSeedEntry("सूल", "70", "संज्ञा", "नुकीला भाला, शूल", "spear, pointed spike", "sool", "B", "लोहे का नुकीला सूल"),
        RawSeedEntry("सेल", "71", "संज्ञा", "बैटरी सेल, कोशिका", "battery cell, biology cell", "sel", "B", "घड़ी का गोल सेल"),
        RawSeedEntry("सो", "72", "क्रिया", "सोना, शयन करना", "sleep", "so", "A", "रात को समय पर सो जाना"),
        RawSeedEntry("सील", "73", "संज्ञा", "मुहर, बंद करने की छाप", "seal, stamp", "seel", "B", "सरकारी लिफाफे पर सील"),
        RawSeedEntry("सरल", "74", "विशेषण", "सीधा, आसान, सहज", "simple, easy, straightforward", "saral", "A", "सरल और सहज स्वभाव"),

        // 75-99
        RawSeedEntry("सप", "75", "संज्ञा", "ध्वनि-अनुकरण शब्द", "slurping sound", "sap", "B", "सप-सप आवाज"),
        RawSeedEntry("सः", "76", "सर्वनाम", "संस्कृत रूप 'वह'", "Sanskrit pronoun 'he'", "sah", "B", "संस्कृत व्याकरण"),
        RawSeedEntry("सस", "77", "संज्ञा", "पुराना lexical शब्द", "lexical archaic root", "sas", "B", "प्राचीन संदर्भ"),
        RawSeedEntry("साल", "78", "संज्ञा", "वर्ष, बारह महीने", "year", "saal", "A", "नया साल मुबारक"),
        RawSeedEntry("सन", "79", "संज्ञा", "सन का रेशा, पौधा", "flax, jute, hemp", "san", "A", "सन की मजबूत रस्सी"),
        RawSeedEntry("आलू", "80", "संज्ञा", "एक लोकप्रिय कंद सब्जी", "potato", "aaloo", "A", "गरम-गरम उबला आलू"),
        RawSeedEntry("आग", "81", "संज्ञा", "अग्नि, पावक", "fire, flame", "aag", "A", "चूल्हे में जलती आग"),
        RawSeedEntry("बोल", "82", "संज्ञा/क्रिया", "वाणी, वचन, बोलना", "speech, words, speech sound", "bol", "A", "मीठे और सच्चे बोल"),
        RawSeedEntry("भील", "83", "संज्ञा", "भारत का प्राचीन समुदाय", "Bheel community", "bheel", "B", "ऐतिहासिक धनुर्धर समुदाय"),
        RawSeedEntry("आज", "84", "अव्यय", "वर्तमान दिन", "today", "aaj", "A", "आज का सुनहरा दिन"),
        RawSeedEntry("हार", "84", "संज्ञा", "फूलों या मोतियों की माला", "garland, necklace", "haar", "A", "चमकते मोतियों का सुंदर हार", mnemonicStory = "मखमली डिब्बे में हीरे और मोतियों से जड़ा एक शाही 'हार' जगमगा रहा है।"),
        RawSeedEntry("आप", "85", "सर्वनाम", "आदरसूचक मध्यम पुरुष सर्वनाम", "you (honorific)", "aap", "A", "आप यहाँ पधारें"),
        RawSeedEntry("बछ", "86", "संज्ञा", "गाय का बछड़ा (क्षेत्रीय)", "calf", "bachh", "B", "गाय का नन्हा बछड़ा"),
        RawSeedEntry("आस", "87", "संज्ञा", "आशा, उम्मीद, भरोसा", "hope, expectation", "aas", "A", "मन में नई आस जागना"),
        RawSeedEntry("बाल", "88", "संज्ञा", "केश, सिर के बाल, बालक", "hair, child", "baal", "A", "काले घने बाल"),
        RawSeedEntry("आम", "89", "संज्ञा", "फलों का राजा आम", "mango", "aam", "A", "पका हुआ रसीला आम"),
        RawSeedEntry("मूल", "90", "संज्ञा", "जड़, आधार, बुनियादी", "root, foundation, origin", "mool", "A", "पेड़ की गहरी मूल"),
        RawSeedEntry("मेल", "91", "संज्ञा", "मिलन, एकता, सामंजस्य", "union, harmony, accord", "mel", "A", "आपसी मेल-जोल"),
        RawSeedEntry("मोल", "92", "संज्ञा", "कीमत, मूल्य", "price, worth, value", "mol", "A", "अनमोल वस्तु का मोल"),
        RawSeedEntry("नील", "93", "संज्ञा", "नीला रंग, नील का पौधा", "indigo, blue color", "neel", "B", "गहरा नील रंग"),
        RawSeedEntry("नहर", "94", "संज्ञा", "सिंचाई की कृत्रिम जलधारा", "canal", "nahar", "A", "खेतों से गुजरती नहर"),
        RawSeedEntry("अमर", "94", "विशेषण", "कभी न मिटने वाला, चिरंजीवी", "immortal, eternal", "amar", "A", "अमर शहीदों की स्मृति"),
        // 95 is empty in V2.3
        RawSeedEntry("मछ", "96", "संज्ञा", "मछली का क्षेत्रीय रूप", "fish (dialect form)", "machh", "B", "पानी की मछ"),
        RawSeedEntry("मसल", "97", "क्रिया", "मसलना, दबाना", "crush, knead, press", "masal", "A", "हाथ से मसलना"),
        RawSeedEntry("माल", "98", "संज्ञा", "सामान, वस्तुएं, धन", "goods, merchandise, cargo", "maal", "A", "नाव में भरा हुआ माल"),
        RawSeedEntry("नाव", "98", "संज्ञा", "नौका, जलयान", "boat", "naav", "A", "नदी में तैरती नाव"),
        RawSeedEntry("मन", "99", "संज्ञा", "हृदय, चित्त, अंतःकरण", "mind, heart, inner self", "man", "A", "मन की शांति"),

        // Additional regression & 3+ digit words from V2.3 Tables 6, 12, 14 and extended vocabulary
        RawSeedEntry("खुश", "100", "विशेषण", "प्रसन्न, आनंदित, हर्षित", "happy, joyful", "khush", "A", "खिलखिलाता हुआ खुश चेहरा", mnemonicStory = "सौ में से सौ अंक पाकर एक विद्यार्थी खुशी से हवा में उछल रहा है और बेहद 'खुश' है।"),
        RawSeedEntry("कुहू", "100", "संज्ञा", "कोयल की मीठी आवाज (कुहू-कुहू)", "cuckoo call", "kuhoo", "A", "बाग में गूँजती कोयल की कुहू"),
        RawSeedEntry("कूद", "102", "क्रिया/संज्ञा", "छलांग लगाना, कूदना", "jump, leap", "kood", "A", "ऊँची पहाड़ी से रोमांचक कूद"),
        RawSeedEntry("गुड़", "102", "संज्ञा", "गन्ने के रस से बना मीठा खाद्य", "jaggery", "gud", "A", "मिट्टी की हांडी में मीठा देसी गुड़"),
        RawSeedEntry("कुली", "103", "संज्ञा", "स्टेशन पर सामान उठाने वाला", "porter", "kulee", "A", "लाल कुर्ते वाला कुली"),
        RawSeedEntry("कूप", "105", "संज्ञा", "कुआँ, जल का गहरा स्रोत", "well, deep water pit", "koop", "A", "पत्थर से बना प्राचीन गहरा कूप"),
        RawSeedEntry("कुछ", "106", "सर्वनाम/विशेषण", "थोड़ा, कोई वस्तु", "some, a few, something", "kuchh", "A", "हथेली में रखे कुछ चमकते सिक्के"),
        RawSeedEntry("कुआँ", "108", "संज्ञा", "पानी निकालने का गहरा गड्ढा/कुआँ", "water well", "kuaan", "A", "गाँव के चौक का ठंडे पानी वाला कुआँ"),
        RawSeedEntry("गुण", "109", "संज्ञा", "विशेषता, खूबी, सद्गुण", "virtue, quality, trait", "gun", "A", "हीरे की तरह चमकते सद्गुण"),
        RawSeedEntry("गेहूँ", "110", "संज्ञा", "प्रमुख खाद्यान्न", "wheat", "gehoon", "A", "खेत में लहराता गेहूँ", mnemonicStory = "सूरज की धूप में सोने जैसा चमकता हुआ 'गेहूँ' का विशाल खेत हवा में लहरा रहा है।"),
        RawSeedEntry("केक", "111", "संज्ञा", "मीठा बेक किया पकवान", "cake", "kek", "A", "जन्मदिन का मीठा केक", mnemonicStory = "मेज़ पर तीन मंज़िला विशाल चॉकलेट 'केक' रखा है जिस पर रंग-बिरंगी मोमबत्तियाँ जल रही हैं।"),
        RawSeedEntry("गोद", "122", "संज्ञा", "अंक, गोद", "lap", "god", "A", "माँ की ममतामयी गोद"),
        RawSeedEntry("काग", "181", "संज्ञा", "कौआ, काक पक्षी", "crow, raven", "kaag", "A", "मुंडेर पर बैठा काला काग"),
        RawSeedEntry("खाद", "182", "संज्ञा", "खेत की उर्वरक खाद", "fertilizer, compost", "khaad", "A", "हरी फसल के लिए उपजाऊ खाद"),
        RawSeedEntry("घाट", "183", "संज्ञा", "नदी का पक्का किनारा या सीढ़ियाँ", "river bank steps, ghat", "ghaat", "A", "गंगा नदी का दीपों से सजा घाट"),
        RawSeedEntry("कार", "184", "संज्ञा", "चार पहियों की मोटर गाड़ी", "car, automobile", "kaar", "A", "तेज रफ़्तार लाल स्पोर्ट्स कार"),
        RawSeedEntry("गांव", "185", "संज्ञा", "ग्रामीण बस्ती (अनुस्वार वर्तनी)", "village (anusvara spelling)", "gaanv", "A", "हरे-भरे खेतों वाला सुंदर गांव", mnemonicStory = "नदी के किनारे बसा एक सुंदर 'गांव' देखो जहाँ मिट्टी के घरों और आम के पेड़ों की कतार है।"),
        RawSeedEntry("काँप", "185", "क्रिया", "काँपना, थरथराना", "shiver, tremble", "kaanp", "A", "ठंड या रोमांच से काँपना", mnemonicStory = "बर्फीली हवा चलते ही सूखे पत्ते ज़ोर-ज़ोर से 'काँप' रहे हैं।"),
        RawSeedEntry("घास", "187", "संज्ञा", "मैदान की हरी दूब/घास", "grass, lawn", "ghaas", "A", "ओस से भीगी मखमली हरी घास"),
        RawSeedEntry("गायब", "188", "विशेषण", "अदृश्य, लुप्त होना", "vanished, invisible", "gaayab", "A", "जादूगर की टोपी से खरगोश गायब"),
        RawSeedEntry("काम", "189", "संज्ञा", "कार्य, मेहनत, जिम्मेदारी", "work, task, deed", "kaam", "A", "सफलता दिलाने वाला नेक काम"),
        RawSeedEntry("कान", "189", "संज्ञा", "सुनने की इंद्रिय, कर्ण", "ear", "kaan", "A", "सोने की बाली पहने हुए कान"),
        RawSeedEntry("कमर", "194", "संज्ञा", "शरीर का मध्य भाग, कटि", "waist", "kamar", "A", "कमर पर बंधी सोने की पेटी"),
        RawSeedEntry("दूध", "202", "संज्ञा", "गाय-भैंस का पौष्टिक सफेद पेय", "milk", "doodh", "A", "चाँदी के गिलास में गर्म दूध"),
        RawSeedEntry("दूत", "203", "संज्ञा", "संदेशवाहक, राजदूत", "messenger, envoy", "doot", "A", "घोड़े पर सवार शाही दूत"),
        RawSeedEntry("दूर", "204", "विशेषण/अव्यय", "अधिक फासले पर, सुदूर", "far, distant", "door", "A", "दूर पहाड़ियों पर चमकता सूरज"),
        RawSeedEntry("धूप", "205", "संज्ञा", "सूर्य का प्रकाश, सुगंधित धूप", "sunlight, incense", "dhoop", "A", "सर्दियों की सुनहरी गुनगुनी धूप"),
        RawSeedEntry("दुआ", "208", "संज्ञा", "प्रार्थना, आशीर्वाद", "prayer, blessing", "duaa", "A", "माँ के हाथ से निकली सच्ची दुआ"),
        RawSeedEntry("दुलहन", "209", "संज्ञा", "नववधू, विवाह की वधू", "bride", "dulhan", "A", "लाल जोड़े में सजी सुंदर दुलहन"),
        RawSeedEntry("धुन", "209", "संज्ञा", "संगीत की लय, लगन", "melody, tune, passion", "dhun", "A", "बांसुरी की मनमोहक धुन"),
        RawSeedEntry("देख", "211", "क्रिया", "देखना, दृष्टिपात", "look, see", "dekh", "A", "ध्यान से देखना"),
        RawSeedEntry("दीया", "238", "संज्ञा", "मिट्टी का दीपक", "clay lamp, earthen light", "deeyaa", "A", "दीवाली का जगमगाता दीया"),
        RawSeedEntry("शोला", "028", "संज्ञा", "आग की दहकती लपट, अंगारा", "flame, blazing ember", "shola", "A", "दहकता हुआ लाल शोला"),
        RawSeedEntry("शीत", "033", "संज्ञा/विशेषण", "ठंडक, जाड़ा, शीतल", "cold, winter", "sheet", "A", "पहाड़ों की ठंडी शीत लहर"),
        RawSeedEntry("ठेला", "318", "संज्ञा", "हाथ से चलाने वाली चार पहिया गाड़ी", "handcart", "thelaa", "A", "ताजे फलों से सजा रंगीन ठेला"),
        RawSeedEntry("तोप", "325", "संज्ञा", "युद्ध का बड़ा भारी अस्त्र", "cannon", "top", "A", "किले पर रखी भारी तोप"),
        RawSeedEntry("तपः", "356", "संज्ञा", "तपस्या, साधना (विसर्ग युक्त संस्कृतनिष्ठ रूप)", "penance, spiritual austerity", "tapah", "B", "ऋषि की तेजस्वी तपः साधना", mnemonicStory = "हिमालय की शांत गुफा में एक तेजस्वी ऋषि गहन 'तपः' साधना में लीन हैं और उनके चारों ओर दिव्य प्रकाश है।"),
        RawSeedEntry("तार", "384", "संज्ञा", "धातु का तार, सितार का तार", "wire, string, telegram", "taar", "A", "सितार का झंकृत होता सुनहरा तार"),
        RawSeedEntry("तान", "389", "संज्ञा", "संगीत का स्वर-विस्तार", "musical note, melody pitch", "taan", "A", "गायक की ऊँची सुरीली तान"),
        RawSeedEntry("रूप", "405", "संज्ञा", "सौंदर्य, आकार, चेहरा", "beauty, form, appearance", "roop", "A", "दर्पण में झलकता सुंदर रूप"),
        RawSeedEntry("जूस", "407", "संज्ञा", "फलों का ताज़ा रस", "fruit juice", "joos", "A", "बर्फ डाला हुआ संतरे का ताज़ा जूस"),
        RawSeedEntry("चूहा", "408", "संज्ञा", "छोटा कुतरने वाला जीव", "mouse, rat", "choohaa", "A", "बिल से निकला नन्हा चूहा"),
        RawSeedEntry("रीझ", "436", "संज्ञा/क्रिया", "प्रसन्न होना, मुग्ध होना", "delight, fascination", "reejh", "B", "सुंदर कला पर मन का रीझ जाना"),
        RawSeedEntry("चार", "484", "संख्या", "संख्या चार (4)", "number four", "chaar", "A", "चौराहे के चार रास्ते"),
        RawSeedEntry("जान", "489", "संज्ञा", "प्राण, जीवन", "life, soul", "jaan", "A", "मुस्कुराती हुई जिंदादिल जान"),
        RawSeedEntry("चना", "498", "संज्ञा", "चने की दाल, चना", "gram, chickpea", "chanaa", "A", "भूने हुए चने"),
        RawSeedEntry("फ़िल्म", "539", "संज्ञा", "चलचित्र, सिनेमा", "movie, film", "film", "A", "बड़े पर्दे पर चलती रोचक फ़िल्म"),
        RawSeedEntry("पंख", "551", "संज्ञा", "पक्षी का पर, पंख", "feather, wing", "pankh", "A", "मोर का सुंदर पंख"),
        RawSeedEntry("पंप", "555", "संज्ञा", "पानी खींचने की मशीन", "water pump", "pamp", "A", "खेत में चलता ट्यूबवेल पंप"),
        RawSeedEntry("पान", "589", "संज्ञा", "तांबूल, पान का पत्ता", "betel leaf", "paan", "A", "चाँदी के वर्क लगा बनारसी पान"),
        RawSeedEntry("सूई", "703", "संज्ञा", "सिलाई करने की बारीक धातु की सूई", "needle", "sooee", "A", "चमकती हुई चाँदी की सूई और धागा"),
        RawSeedEntry("सूप", "705", "संज्ञा", "अनाज फटकने का सूप या गर्म सूप", "winnowing basket, soup", "soop", "A", "कटोरे में भाप निकलता गर्म सूप"),
        RawSeedEntry("संग", "751", "संज्ञा/अव्यय", "साथ, संगति", "company, together", "sang", "A", "मित्रों का आनंदमय संग"),
        RawSeedEntry("सात", "783", "संख्या", "संख्या सात (7), सप्त", "number seven", "saat", "A", "आसमान में चमकते इंद्रधनुष के सात रंग"),
        RawSeedEntry("आभा", "888", "संज्ञा", "कांति, चमक, आभा", "radiance, aura, glow", "aabhaa", "A", "चेहरे की सुंदर आभा"),
        RawSeedEntry("बाण", "889", "संज्ञा", "तीर, धनुष का बाण", "arrow", "baan", "A", "सोने के धनुष से छूटा चमकता बाण"),
        RawSeedEntry("नूर", "904", "संज्ञा", "रोशनी, चमक, तेज", "light, radiance", "noor", "A", "चेहरे पर चमकता दिव्य नूर"),
        RawSeedEntry("मोर", "924", "संज्ञा", "राष्ट्रीय पक्षी मयूर", "peacock", "mor", "A", "बारिश में पंख फैलाकर नाचता मोर", mnemonicStory = "काले बादलों के नीचे एक सुंदर रंग-बिरंगा 'मोर' अपने हज़ारों चमकीले पंख फैलाकर नाच रहा है।"),
        RawSeedEntry("मंच", "954", "संज्ञा", "स्टेज, व्याख्यान पीठ", "stage, podium", "manch", "A", "सजा हुआ सुंदर मंच"),
        RawSeedEntry("नाम", "989", "संज्ञा", "पहचान, संज्ञा, ख्याति", "name, fame", "naam", "A", "स्वर्ण अक्षरों में लिखा चमकता नाम"),
        RawSeedEntry("ज़मीर", "0934", "संज्ञा", "अंतरात्मा, विवेक", "conscience, inner voice", "zameer", "A", "सच्चे इंसान का जागता ज़मीर"),
        RawSeedEntry("कुहुक", "1001", "संज्ञा", "कोयल की मधुर कूक, कुहुकना", "sweet cooing of cuckoo", "kuhuk", "A", "आम की डाली पर कोयल की मीठी कुहुक", mnemonicStory = "वसंत ऋतु में आम के पेड़ की सबसे ऊँची डाली पर बैठी कोयल की मधुर 'कुहुक' पूरे बाग में गूँज रही है।"),
        RawSeedEntry("कुकी", "1013", "संज्ञा", "कुरकुरा बिस्कुट", "cookie, biscuit", "kukee", "A", "चॉकलेट कुकी"),
        RawSeedEntry("कमरा", "1948", "संज्ञा", "कक्ष, भवन का कमरा", "room, chamber", "kamraa", "A", "रोशनी से भरा सुंदर कमरा"),
        RawSeedEntry("कमलरस", "1947", "संज्ञा (यौगिक)", "कमल के फूल का मधुर रस (मकरंद)", "lotus nectar (kamal + ras)", "kamalras", "B", "खिले हुए कमल से टपकता सुनहरा रस", mnemonicStory = "शांत सरोवर में खिले एक विशाल गुलाबी 'कमल' (19) की पंखुड़ियों से सुनहरा मीठा 'रस' (47) टपक रहा है — यही 'कमलरस' (1947) है!"),
        RawSeedEntry("धूलोझल", "2026", "विशेषण (यौगिक)", "धूल के गुबार में ओझल (छिपा हुआ)", "hidden in a cloud of dust (dhool + ojhal)", "dhoolojhal", "B", "सुनहरी धूल के बादल में ओझल होता रथ", mnemonicStory = "रेगिस्तान की चमकती सुनहरी 'धूल' (20) के बवंडर में एक भव्य महल अचानक आँखों से 'ओझल' (26) होकर 'धूलोझल' (2026) हो गया!"),
        RawSeedEntry("रोटी", "4233", "संज्ञा", "गेहूँ की चपाती", "bread, flatbread", "rotee", "A", "तवे की गर्म रोटी"),
        RawSeedEntry("चाबी", "4883", "संज्ञा", "ताला खोलने की कुंजी", "key", "chaabee", "A", "खजाने के ताले की सुनहरी चाबी"),
        RawSeedEntry("पानी", "5893", "संज्ञा", "जल, पेय पदार्थ", "water", "paanee", "A", "शीतल स्वच्छ पानी"),
        RawSeedEntry("सोना", "7298", "संज्ञा", "स्वर्ण धातु या निद्रा", "gold, sleep", "sonaa", "A", "चमकदार पीला सोना"),
        RawSeedEntry("मोती", "9233", "संज्ञा", "सीप का चमकदार रत्न", "pearl", "motee", "A", "सफेद चमकदार मोती")
    )

    fun buildVividMnemonicStory(
        word: String,
        code: String,
        meaningHi: String,
        visualAssociation: String?,
        existingStory: String? = null
    ): String {
        if (!existingStory.isNullOrBlank()) return existingStory
        val visual = visualAssociation?.takeIf { it.isNotBlank() } ?: "$word ($meaningHi)"
        return "कल्पना करें: आपके सामने एक विशाल और चमकदार '$word' ($visual) है, जिस पर सुनहरे अक्षरों में संख्या $code चमक रही है।"
    }

    fun buildWhyGoodPeg(
        word: String,
        code: String,
        category: String,
        meaningHi: String,
        qualityGrade: String,
        existingReason: String? = null
    ): String {
        if (!existingReason.isNullOrBlank()) return existingReason
        return when (qualityGrade.uppercase()) {
            "A" -> "'$word' ($meaningHi) रोज़मर्रा की हिंदी का एक सुस्पष्ट, ठोस और तुरंत दृश्य बनने वाला A-स्तर का पेग शब्द है जिसका V2.3 कोड ठीक $code निकलता है।"
            "B" -> "'$word' ($meaningHi) V2.3 नियमों से ठीक $code कोड देता है (B-स्तर: साहित्यिक/यौगिक/विशेष संदर्भ में उपयोगी पेग)।"
            else -> "'$word' ($meaningHi) के अक्षर V2.3 नियमों के अनुसार ठीक संख्या $code को एनकोड करते हैं।"
        }
    }

    /**
     * Converts raw seed entries into fully validated PegWord domain entities.
     * Re-calculates and verifies the code for each entry deterministically!
     */
    fun getValidatedSeedPegs(ruleSet: EncodingRuleSet = EncodingRuleSet.V2_3): List<PegWord> {
        return SEED_ENTRIES.map { raw ->
            val enc = PhoneticMatraEncoder.encode(raw.word, ruleSet)
            // Recalculated code is the authoritative code
            val calculatedCode = enc.code
            val story = buildVividMnemonicStory(
                word = raw.word,
                code = calculatedCode,
                meaningHi = raw.meaningHi,
                visualAssociation = raw.visualAssociation,
                existingStory = raw.mnemonicStory
            )
            val whyPeg = buildWhyGoodPeg(
                word = raw.word,
                code = calculatedCode,
                category = raw.category,
                meaningHi = raw.meaningHi,
                qualityGrade = raw.qualityGrade,
                existingReason = raw.whyGoodPeg
            )
            PegWord(
                id = "seed_${raw.word}_$calculatedCode",
                word = raw.word,
                normalizedWord = enc.normalizedWord,
                code = calculatedCode,
                ruleSetId = ruleSet.id,
                category = raw.category,
                meaningHi = raw.meaningHi,
                meaningEn = raw.meaningEn,
                transliteration = raw.transliteration,
                qualityGrade = raw.qualityGrade,
                verificationStatus = if (calculatedCode == raw.expectedCode) "approved" else "unverified",
                verificationSource = "v2_3_seed",
                visualAssociation = raw.visualAssociation,
                exampleSentenceHi = raw.exampleSentenceHi,
                exampleSentenceEn = raw.exampleSentenceEn,
                isFavorite = false,
                tags = listOf("v2_3", raw.qualityGrade.lowercase(), raw.category),
                mnemonicStory = story,
                encodingBreakdown = enc.formattedTraceSummary(),
                encoderVersion = ruleSet.version,
                dictionaryEvidence = whyPeg,
                commonness = if (raw.qualityGrade == "A") "high" else "medium",
                confidence = if (raw.qualityGrade == "A") 1.0f else 0.85f
            )
        }
    }
}
