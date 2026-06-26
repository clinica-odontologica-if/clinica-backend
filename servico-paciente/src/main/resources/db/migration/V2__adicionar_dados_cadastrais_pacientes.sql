ALTER TABLE pacientes ADD COLUMN cpf VARCHAR(11);
ALTER TABLE pacientes ADD COLUMN data_nascimento DATE;
ALTER TABLE pacientes ADD COLUMN endereco VARCHAR(255);
ALTER TABLE pacientes ADD COLUMN observacoes VARCHAR(500);

ALTER TABLE pacientes MODIFY COLUMN email VARCHAR(150) NULL;

UPDATE pacientes
SET cpf = CONCAT('SEMCPF', LPAD(id, 5, '0'))
WHERE cpf IS NULL;

UPDATE pacientes
SET data_nascimento = '1900-01-01'
WHERE data_nascimento IS NULL;

ALTER TABLE pacientes MODIFY COLUMN cpf VARCHAR(11) NOT NULL;
ALTER TABLE pacientes MODIFY COLUMN data_nascimento DATE NOT NULL;
ALTER TABLE pacientes ADD CONSTRAINT uk_pacientes_cpf UNIQUE (cpf);
