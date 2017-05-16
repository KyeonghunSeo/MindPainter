package com.hellowo.mindpainter;

public class Question {
    public static int question_level = 0;
    public static int question_num = 0;

    final static String[][] questions = {

            // LEVEL 1
            {
                    "훈민정음","거위","미녀와야수","오늘의유머","귀저기","괴기소설","오빠부대","부대찌개",
                    "김치","한겨울","추석","모바일","다이어리","침대","자동차","당직근무","박격포","핵무기",
                    "김정은","문재인","박근혜","투표","사랑","나얼","아이폰","안드로이드","하늘","고무줄",
                    "햄버거","스테이크","피자","파스타","짬뽕","잔치국수","별자리","잠자리","어벤저스",
                    "아이언맨","헐크","호크아이","캡틴아메리카","블랙위도우","로키","토프","가디언즈오브갤럭시",
                    "고래밥","새우깡","상어","쇼미더머니","블랙넛","송민호","도끼","선녀","나무","천사","신발",
                    "선생님","초등학교","사용자","설명서","활화산","활어회","회복마법","암흑마법","치료제",
                    "구술면접","심층면접","중국","미세먼지","구글","게임중독","약물복용","구남친","전남친",
                    "연애시대","연예인","한류스타","강남스타일","말달리자","말리꽃","꽃","귀마개","물안경","수영",
                    "강강술래","파리의연인","한석규","쉬리","맘마미아","양들의침묵","볼펜","가위","망망대해",
                    "아마존","아무무","미이라","공룡","석기시대","피구왕통키","가오리","마이크","구미호","아리",
                    "빅토르","리그오브레전드","오버워치","키자루","원피스","루피","소맥","폭탄주","구리","폭탄",
                    "라면","냉면","쫄면","떡볶이","순대","자아성찰","명상","리신","조준사격","비밀","성형수술",
                    "다이어트","거울","탄핵","보도","청와대","거짓말","빅뱅","지드래곤","안경","맥주","독일",
                    "전문가","뉴스","최순실","반장선거","디즈니랜드","인어공주","모아나","영어공부","토익시험",
                    "교도관","도둑","오댕","영한사전","한영사전","미운오리새끼","김건모","고난","명탐정","과거",
                    "인터스텔라","블랙홀","우주선","승마","대기업","삼성","갤럭시","선풍기","고3","집중력",
                    "지구력","헬스장","근육","각선미"
            },

            // LEVEL 2
            {

            },

            // LEVEL 3
            {

            }

    };

    public static String selectQuestion(int level, int num) {
        question_level = level;
        question_num = num;
        return questions[level][num];
    }

    public static String selectNewQuestion(int level) {
        int num = (int) (Math.random() * (questions[level].length));
        if(level == question_level && num == question_num) {
            return selectNewQuestion(level);
        }else{
            return selectQuestion(level, num);
        }
    }

    public static String getCurrentQuestion() {
        return questions[question_level][question_num];
    }
}
